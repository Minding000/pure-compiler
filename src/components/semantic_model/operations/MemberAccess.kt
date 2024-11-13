package components.semantic_model.operations

import components.code_generation.llvm.models.operations.MemberAccess
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.scopes.Scope
import components.semantic_model.types.*
import components.semantic_model.values.*
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
}
