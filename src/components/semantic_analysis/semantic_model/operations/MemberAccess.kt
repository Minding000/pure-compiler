package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.*
import components.semantic_analysis.semantic_model.values.InitializerReference
import components.semantic_analysis.semantic_model.values.SelfReference
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.issues.access.GuaranteedAccessWithNullCheck
import logger.issues.access.OptionalAccessWithoutNullCheck
import java.util.*
import components.syntax_parser.syntax_tree.access.MemberAccess as MemberAccessSyntaxTree

class MemberAccess(override val source: MemberAccessSyntaxTree, scope: Scope, val target: Value, val member: Value,
				   private val isOptional: Boolean): Value(source, scope) {

	init {
		addUnits(target, member)
	}

	override fun linkValues(linter: Linter) {
		target.linkValues(linter)
		target.type?.let { targetTypeVal ->
			var targetType = targetTypeVal
			if(targetType is OptionalType) {
				if(!isOptional)
					linter.addIssue(OptionalAccessWithoutNullCheck(source, targetType))
				targetType = targetType.baseType
			} else {
				if(isOptional)
					linter.addIssue(GuaranteedAccessWithNullCheck(source, targetType))
			}
			member.scope = targetType.interfaceScope
			member.linkValues(linter)
			member.type?.let { memberType ->
				type = if(isOptional && memberType !is OptionalType)
					OptionalType(source, scope, memberType)
				else
					memberType
				if(!isOptional)
					staticValue = member.staticValue
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(target is SelfReference)
			member.analyseDataFlow(tracker)
		else
			target.analyseDataFlow(tracker)
	}

	fun filterForPossibleTargetTypes(linter: Linter, availableTypes: List<ObjectType>): List<Type> {
		val possibleTargetTypes = LinkedList<Type>()
		for(availableType in availableTypes) {
			when(member) {
				is InitializerReference -> {
					val staticType = StaticType(availableType.definition ?: continue)
					val functionCall = parent as? FunctionCall ?: continue
					if(staticType.resolveInitializer(linter, listOf(), listOf(), functionCall.typeParameters, functionCall.valueParameters)
						== null)
						continue
					possibleTargetTypes.add(staticType)
				}
				is VariableValue -> {
					val parent = parent
					if(parent is FunctionCall) {
						val functionType = availableType.interfaceScope.resolveValue(member)?.type as? FunctionType? ?: continue
						val functionCall = parent as? FunctionCall ?: continue
						if(functionType.resolveSignature(linter, functionCall.typeParameters, functionCall.valueParameters) == null)
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
}
