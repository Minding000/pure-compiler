package components.semantic_model.operations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.types.*
import components.semantic_model.values.*
import errors.internal.CompilerError
import logger.issues.access.GuaranteedAccessWithHasValueCheck
import logger.issues.access.OptionalAccessWithoutHasValueCheck
import java.util.*
import components.syntax_parser.syntax_tree.access.MemberAccess as MemberAccessSyntaxTree

class MemberAccess(override val source: MemberAccessSyntaxTree, scope: Scope, val target: Value, val member: Value,
				   private val isOptional: Boolean): Value(source, scope) {

	init {
		addSemanticModels(target, member)
	}

	override fun determineTypes() {
		target.determineTypes()
		var targetType = target.type
		if(targetType != null) {
			if(targetType is OptionalType)
				targetType = targetType.baseType
			member.scope = targetType.interfaceScope
			member.determineTypes()
			var memberType = member.type
			if(memberType != null) {
				memberType = memberType.getLocalType(this, targetType)
				type = if(isOptional && memberType !is OptionalType)
					OptionalType(source, scope, memberType)
				else
					memberType
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(target is SelfReference) {
			member.analyseDataFlow(tracker)
		} else {
			target.analyseDataFlow(tracker)
			//TODO write test to make sure this is fine
			(member as? VariableValue)?.computeValue(tracker)
		}
		//TODO use dataflow analysis result here instead of static type
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
					if(staticType.getInitializer(emptyList(), emptyList(), functionCall.typeParameters, functionCall.valueParameters) == null)
						continue
					possibleTargetTypes.add(staticType)
				}
				is VariableValue -> {
					val parent = parent
					if(parent is FunctionCall) {
						val (_, type) = availableType.interfaceScope.getValueDeclaration(member)
						val functionType = type as? FunctionType? ?: continue
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

	override fun getLlvmLocation(constructor: LlvmConstructor): LlvmValue {
		if(member !is VariableValue)
			throw CompilerError(source, "Member access references invalid member of type '${member.javaClass.simpleName}'.")
		if(member.declaration is ComputedPropertyDeclaration)
			throw CompilerError(source, "Computed properties do not have a location.")
		val targetValue = target.getLlvmValue(constructor)
		var targetType = target.type
		if(targetType is OptionalType)
			targetType = targetType.baseType
		val llvmTargetType = when(targetType) {
			is ObjectType -> targetType.getTypeDeclaration()?.llvmType
			is StaticType -> targetType.typeDeclaration.llvmStaticType
			//TODO support member accesses on union types
			else -> throw CompilerError(source,
				"Member access target of type '${targetType?.javaClass?.simpleName}' is not an object or class.")
		}
		return context.resolveMember(constructor, llvmTargetType, targetValue, member.name,
			(member.declaration as? InterfaceMember)?.isStatic ?: false)
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val memberType = member.type
		val memberLlvmType = memberType?.getLlvmType(constructor)
		return if(isOptional) {
			val resultLlvmType = constructor.pointerType
			val targetValue = target.getLlvmValue(constructor)
			val result = constructor.buildStackAllocation(resultLlvmType, "_optionalMemberAccessResultVariable")
			val function = constructor.getParentFunction()
			val valueBlock = constructor.createBlock(function, "_optionalMemberAccessValueBlock")
			val nullBlock = constructor.createBlock(function, "_optionalMemberAccessNullBlock")
			val resultBlock = constructor.createBlock(function, "_optionalMemberAccessResultBlock")
			constructor.buildJump(constructor.buildIsNull(targetValue, "_isTargetNull"), nullBlock, valueBlock)
			constructor.select(nullBlock)
			constructor.buildStore(constructor.nullPointer, result)
			constructor.buildJump(resultBlock)
			constructor.select(valueBlock)
			val memberValue = getMemberValue(constructor, memberLlvmType)
			if(memberType?.isLlvmPrimitive() == true) {
				//TODO copy all (optional) primitives (or value-objects) on use
				// - assignment - DONE
				// - function argument
				//   - binary operator calls
				//   - index assignment
				// - return
				val box = constructor.buildHeapAllocation(memberLlvmType, "_optionalPrimitiveBox")
				constructor.buildStore(memberValue, box)
				constructor.buildStore(box, result)
			} else {
				constructor.buildStore(memberValue, result)
			}
			constructor.buildJump(resultBlock)
			constructor.select(resultBlock)
			constructor.buildLoad(resultLlvmType, result, "_optionalMemberAccessResult")
		} else {
			getMemberValue(constructor, memberLlvmType)
		}
	}

	private fun getMemberValue(constructor: LlvmConstructor, memberLlvmType: LlvmType?): LlvmValue {
		val declaration = (member as? VariableValue)?.declaration
		return if(declaration is ComputedPropertyDeclaration) {
			val setStatement = declaration.setStatement
			if(setStatement != null && isIn(setStatement))
				constructor.getLastParameter()
			else
				buildGetterCall(constructor, declaration)
		} else {
			constructor.buildLoad(memberLlvmType, getLlvmLocation(constructor), "member")
		}
	}

	private fun buildGetterCall(constructor: LlvmConstructor, computedPropertyDeclaration: ComputedPropertyDeclaration): LlvmValue {
		val targetValue = target.getLlvmValue(constructor)
		val functionAddress = context.resolveFunction(constructor, computedPropertyDeclaration.parentTypeDeclaration.llvmType, targetValue,
			computedPropertyDeclaration.getterIdentifier)
		val exceptionAddressLocation = constructor.buildStackAllocation(constructor.pointerType, "exceptionAddress")
		return constructor.buildFunctionCall(computedPropertyDeclaration.llvmGetterType, functionAddress,
			listOf(exceptionAddressLocation, targetValue), "_computedPropertyGetterResult")
		//TODO if exception exists
		// check for optional try (normal and force try have no effect)
		// check for catch
		// resume raise
	}
}
