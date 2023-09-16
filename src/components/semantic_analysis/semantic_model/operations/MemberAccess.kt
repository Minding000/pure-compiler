package components.semantic_analysis.semantic_model.operations

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.*
import components.semantic_analysis.semantic_model.values.*
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
			member.type?.let { memberType ->
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
						if(!availableType.interfaceScope.hasInterfaceMember(member.name))
							continue
					}
					possibleTargetTypes.add(availableType)
				}
			}
		}
		return possibleTargetTypes
	}

	fun getLlvmLocation(constructor: LlvmConstructor): LlvmValue {
		if(member !is VariableValue)
			throw CompilerError(source, "Member access references invalid member of type '${member.javaClass.simpleName}'.")
		val targetValue = target.getLlvmValue(constructor)
		val llvmTargetType = when(val targetType = target.type) {
			is ObjectType -> targetType.getTypeDeclaration()?.llvmType
			is StaticType -> targetType.typeDeclaration.llvmStaticType
			else -> throw CompilerError(source,
				"Member access target of type '${targetType?.javaClass?.simpleName}' is not an object or class.")
		}
		return context.resolveMember(constructor, llvmTargetType, targetValue, member.name,
			(member.declaration as? InterfaceMember)?.isStatic ?: false)
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		return constructor.buildLoad(member.type?.getLlvmType(constructor), getLlvmLocation(constructor), "member")
	}
}
