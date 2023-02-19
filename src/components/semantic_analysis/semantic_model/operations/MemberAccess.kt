package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.values.InitializerReference
import components.semantic_analysis.semantic_model.values.SelfReference
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import messages.Message
import components.syntax_parser.syntax_tree.access.MemberAccess as MemberAccessSyntaxTree

class MemberAccess(override val source: MemberAccessSyntaxTree, val target: Value, val member: Value, private val isOptional: Boolean):
	Value(source) {

	init {
		addUnits(target, member)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		target.linkValues(linter, scope)
		target.type?.let { targetTypeVal ->
			var targetType = targetTypeVal
			if(targetType is OptionalType) {
				if(!isOptional)
					linter.addMessage(source, "Cannot access member of optional type '$targetType' without null check.",
						Message.Type.ERROR)
				targetType = targetType.baseType
			} else {
				if(isOptional)
					linter.addMessage(source, "Optional member access on guaranteed type '$targetType' is unnecessary.",
						Message.Type.WARNING)
			}
			member.linkValues(linter, targetType.scope)
			member.type?.let { memberType ->
				type = if(isOptional && memberType !is OptionalType)
					OptionalType(source, memberType)
				else
					memberType
				if(!isOptional)
					staticValue = member.staticValue
			}
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		if(target is SelfReference)
			member.analyseDataFlow(linter, tracker)
		else
			target.analyseDataFlow(linter, tracker)
	}

	fun filterForPossibleTargetTypes(availableTypes: List<ObjectType>): List<ObjectType> {
		return availableTypes.filter { availableType ->
			when(member) {
				is InitializerReference -> {
					val staticType = StaticType(availableType.definition ?: return@filter false)
					val functionCall = parent as? FunctionCall ?: return@filter false
					staticType.scope.resolveInitializer(listOf(), listOf(), functionCall.typeParameters,
						functionCall.valueParameters) != null
				}
				is VariableValue -> {
					val parent = parent
					if(parent is FunctionCall) {
						val functionType = availableType.scope.resolveValue(member)?.type as? FunctionType? ?: return@filter false
						val functionCall = parent as? FunctionCall ?: return@filter false
						functionType.resolveSignature(functionCall.typeParameters, functionCall.valueParameters) != null
					} else {
						availableType.scope.hasValue(member.name)
					}
				}
				else -> false
			}
		}
	}
}
