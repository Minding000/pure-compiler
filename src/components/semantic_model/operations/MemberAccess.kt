package components.semantic_model.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.operations.MemberAccess
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmType
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.declarations.InterfaceMember
import components.semantic_model.declarations.TypeAlias
import components.semantic_model.scopes.Scope
import components.semantic_model.types.*
import components.semantic_model.values.*
import errors.internal.CompilerError
import logger.issues.access.GuaranteedAccessWithHasValueCheck
import logger.issues.access.OptionalAccessWithoutHasValueCheck
import java.util.*
import components.syntax_parser.syntax_tree.access.MemberAccess as MemberAccessSyntaxTree

class MemberAccess(override val source: MemberAccessSyntaxTree, scope: Scope, val target: Value, val member: Value,
				   val isOptional: Boolean): Value(source, scope) {
	override val hasGenericType: Boolean
		get() = member.hasGenericType

	init {
		addSemanticModels(target, member)
	}

	override fun determineTypes() {
		target.determineTypes()
		//TODO StaticType check is just a quick fix
		var targetType = (if(target.effectiveType is StaticType) target.providedType else target.effectiveType) ?: return
		if(targetType is OptionalType)
			targetType = targetType.baseType
		member.scope = targetType.interfaceScope
		member.determineTypes()
		//TODO StaticType check is just a quick fix
		val memberType =
			(if(member.effectiveType is StaticType) member.providedType else member.effectiveType)?.getLocalType(this, targetType) ?: return
		providedType = if(isOptional && memberType !is OptionalType)
			OptionalType(source, scope, memberType)
		else
			memberType
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(target is SelfReference) {
			member.analyseDataFlow(tracker)
		} else {
			target.analyseDataFlow(tracker)
			//TODO write test to make sure this is fine
			(member as? VariableValue)?.computeValue(tracker)
		}
		setEndStates(tracker)
		val computedTargetType = target.getComputedType()
		if(computedTargetType != null) {
			if(isTypePotentiallyNull(computedTargetType)) {
				if(!isOptional)
					context.addIssue(OptionalAccessWithoutHasValueCheck(source, computedTargetType))
			} else {
				if(isOptional)
					context.addIssue(GuaranteedAccessWithHasValueCheck(source, computedTargetType))
			}
		}
		val computedMemberType = member.getComputedType()
		if(SpecialType.NULL.matches(computedTargetType) || SpecialType.NULL.matches(computedMemberType)) {
			staticValue = NullLiteral(this)
		} else if(computedTargetType !is OptionalType) {
			staticValue = member.getComputedValue()
		}
	}

	private fun isTypePotentiallyNull(type: Type): Boolean {
		return type is OptionalType || SpecialType.NULL.matches(type)
	}

	fun filterForPossibleTargetTypes(availableTypes: List<ObjectType>): List<Type> {
		val possibleTargetTypes = LinkedList<Type>()
		for(availableType in availableTypes) {
			when(member) {
				is InitializerReference -> {
					val staticType = StaticType(availableType.getTypeDeclaration() ?: continue)
					val functionCall = parent as? FunctionCall ?: continue
					if(staticType.getInitializer(emptyList(), emptyList(), functionCall.typeParameters,
							functionCall.valueParameters) == null)
						continue
					possibleTargetTypes.add(staticType)
				}
				is VariableValue -> {
					val parent = parent
					if(parent is FunctionCall) {
						val functionType = availableType.interfaceScope.getValueDeclaration(member)?.type as? FunctionType? ?: continue
						val functionCall = parent as? FunctionCall ?: continue
						if(functionType.getSignature(functionCall.typeParameters, functionCall.valueParameters) == null)
							continue
					} else {
						if(!availableType.interfaceScope.hasValueDeclaration(member.name))
							continue
					}
					possibleTargetTypes.add(availableType)
				}
			}
		}
		return possibleTargetTypes
	}

	override fun toUnit() = MemberAccess(this, target.toUnit(), member.toUnit())

	override fun getLlvmLocation(constructor: LlvmConstructor): LlvmValue {
		if(member !is VariableValue)
			throw CompilerError(source, "Member access references invalid member of type '${member.javaClass.simpleName}'.")
		if(member.declaration is ComputedPropertyDeclaration)
			throw CompilerError(source, "Computed properties do not have a location.")
		val targetType = target.providedType
		if(targetType is StaticType && targetType.typeDeclaration is TypeAlias) {
			val instanceLocation = member.getLlvmLocation(constructor)
			if(instanceLocation != null)
				return instanceLocation
		}
		// Assumption: Primitives don't have properties
		return context.resolveMember(constructor, target.getLlvmValue(constructor), member.name,
			(member.declaration as? InterfaceMember)?.isStatic ?: false)
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val targetType = target.providedType
		return if(isOptional && !(targetType is StaticType && targetType.typeDeclaration is TypeAlias)) {
			val resultLlvmType = constructor.pointerType
			val targetValue = target.getLlvmValue(constructor)
			val result = constructor.buildStackAllocation(resultLlvmType, "_optionalMemberAccess_resultVariable")
			val function = constructor.getParentFunction()
			val valueBlock = constructor.createBlock(function, "_optionalMemberAccess_valueBlock")
			val nullBlock = constructor.createBlock(function, "_optionalMemberAccess_nullBlock")
			val resultBlock = constructor.createBlock(function, "_optionalMemberAccess_resultBlock")
			constructor.buildJump(constructor.buildIsNull(targetValue, "_optionalMemberAccess_isTargetNull"), nullBlock, valueBlock)
			constructor.select(nullBlock)
			constructor.buildStore(constructor.nullPointer, result)
			constructor.buildJump(resultBlock)
			constructor.select(valueBlock)
			var memberValue = getMemberValue(constructor)
			if(member.effectiveType?.isLlvmPrimitive() == true)
				memberValue = ValueConverter.boxPrimitive(constructor, memberValue, member.effectiveType?.getLlvmType(constructor))
			constructor.buildStore(memberValue, result)
			constructor.buildJump(resultBlock)
			constructor.select(resultBlock)
			constructor.buildLoad(resultLlvmType, result, "_optionalMemberAccess_result")
		} else {
			getMemberValue(constructor)
		}
	}

	private fun getMemberValue(constructor: LlvmConstructor): LlvmValue {
		//TODO what about chained member accesses? e.g. player.stats.highScore
		val declaration = (member as? VariableValue)?.declaration
		return if(declaration is ComputedPropertyDeclaration) {
			val setter = declaration.setter
			if(setter != null && isIn(setter))
				constructor.getLastParameter()
			else
				buildGetterCall(constructor, declaration)
		} else {
			constructor.buildLoad(declaration?.effectiveType?.getLlvmType(constructor), getLlvmLocation(constructor), "member")
		}
	}

	private fun buildGetterCall(constructor: LlvmConstructor, computedPropertyDeclaration: ComputedPropertyDeclaration): LlvmValue {
		val exceptionParameter = context.getExceptionParameter(constructor)
		val targetValue = target.getLlvmValue(constructor)
		val llvmType: LlvmType?
		val llvmValue: LlvmValue
		if(target.effectiveType?.isLlvmPrimitive() == true) {
			val primitiveImplementation =
				context.nativeRegistry.resolvePrimitiveImplementation(computedPropertyDeclaration.getGetterSignature())
			llvmType = primitiveImplementation.llvmType
			llvmValue = primitiveImplementation.llvmValue
		} else {
			llvmType = computedPropertyDeclaration.llvmGetterType
			llvmValue = context.resolveFunction(constructor, targetValue, computedPropertyDeclaration.getterIdentifier)
		}
		val returnValue =
			constructor.buildFunctionCall(llvmType, llvmValue, listOf(exceptionParameter, targetValue), "_computedPropertyGetterResult")
		context.continueRaise(constructor, this)
		return returnValue
	}
}
