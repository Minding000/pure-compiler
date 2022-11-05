package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import messages.Message
import components.syntax_parser.syntax_tree.access.MemberAccess as MemberAccessSyntaxTree

class MemberAccess(override val source: MemberAccessSyntaxTree, val target: Value, val member: VariableValue,
				   private val isOptional: Boolean): Value(source) {

	init {
		addUnits(target, member)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		target.linkValues(linter, scope)
		target.type?.let { targetTypeVal ->
			var targetType = targetTypeVal
			if(targetType is OptionalType) {
				if(!isOptional)
					linter.addMessage(source,
						"Cannot access member of optional type '$targetType' without null check.",
						Message.Type.ERROR)
				targetType = targetType.baseType
			} else {
				if(isOptional)
					linter.addMessage(source,
						"Optional member access on guaranteed type '$targetType' is unnecessary.",
						Message.Type.WARNING)
			}
			member.linkValues(linter, targetType.scope)
			member.type?.let { memberType ->
				type = if(isOptional)
					OptionalType(source, memberType)
				else
					memberType
				if(!isOptional)
					staticValue = member.staticValue
			}
		}
	}
}
