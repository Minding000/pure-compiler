package linting.semantic_model.access

import linting.Linter
import linting.semantic_model.literals.QuantifiedType
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import linting.semantic_model.scopes.Scope
import messages.Message
import parsing.syntax_tree.access.MemberAccess

class MemberAccess(override val source: MemberAccess, val target: Value, val member: VariableValue,
				   private val isOptional: Boolean): Value(source) {

	init {
		units.add(target)
		units.add(member)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		target.linkValues(linter, scope)
		target.type?.let { targetType ->
			if(isOptional) {
				if(!(targetType is QuantifiedType && targetType.isOptional))
					linter.addMessage(source,
						"Optional member access on guaranteed type '$targetType' is unnecessary.",
						Message.Type.WARNING)
			} else {
				if(targetType is QuantifiedType && targetType.isOptional)
					linter.addMessage(source,
						"Cannot access member of optional type '$targetType' without null check.",
						Message.Type.ERROR)
			}
			member.linkValues(linter, targetType.scope)
			member.type?.let { baseType ->
				type = if(baseType is QuantifiedType && baseType.isOptional)
					baseType.baseType
				else
					baseType
			}
		}
	}
}