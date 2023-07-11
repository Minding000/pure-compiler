package components.semantic_analysis.semantic_model.operations

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.definitions.MemberDeclaration
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.*
import components.semantic_analysis.semantic_model.values.*
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
		val computedTargetType = target.getComputedType() //TODO use dataflow analysis result here instead of static type
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
					val staticType = StaticType(availableType.definition ?: continue)
					val functionCall = parent as? FunctionCall ?: continue
					if(staticType.resolveInitializer(listOf(), listOf(), functionCall.typeParameters, functionCall.valueParameters)
						== null)
						continue
					possibleTargetTypes.add(staticType)
				}
				is VariableValue -> {
					val parent = parent
					if(parent is FunctionCall) {
						val functionType = availableType.interfaceScope.resolveValue(member)?.type as? FunctionType? ?: continue
						val functionCall = parent as? FunctionCall ?: continue
						if(functionType.resolveSignature(functionCall.typeParameters, functionCall.valueParameters) == null)
							continue
					} else {
						if(!availableType.interfaceScope.hasValue(member.name))
							continue
					}
					possibleTargetTypes.add(availableType)
				}
			}
		}
		return possibleTargetTypes
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		if(member !is VariableValue)
			return super.createLlvmValue(constructor)
		val memberDeclaration = member.definition as? MemberDeclaration ?: return super.getLlvmValue(constructor)
		val targetValue = target.getLlvmValue(constructor)
		val targetType = target.type?.getLlvmType(constructor)
		val targetLocation = constructor.buildAllocation(targetType, "target")
		constructor.buildStore(targetValue, targetLocation)
		val location = constructor.buildGetPropertyPointer(targetType, targetLocation, memberDeclaration.memberIndex, "propertyPointer")
		return constructor.buildLoad(member.type?.getLlvmType(constructor), location, "property")
	}
}
