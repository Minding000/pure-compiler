package linting.semantic_model.operations

import linting.Linter
import linting.semantic_model.scopes.Scope
import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.OptionalType
import linting.semantic_model.values.BooleanLiteral
import linting.semantic_model.values.Value
import messages.Message
import parsing.syntax_tree.operations.NullCheck

class NullCheck(override val source: NullCheck, val value: Value): Value(source) {

	init {
		units.add(value)
		val booleanType = ObjectType(source, Linter.LiteralType.BOOLEAN.className)
		units.add(booleanType)
		type = booleanType
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		for(unit in units)
			if(unit != type)
				unit.linkTypes(linter, scope)
		linter.link(Linter.LiteralType.BOOLEAN, type)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		value.staticValue?.type?.let { staticType ->
			staticValue = if(Linter.LiteralType.NULL.matches(staticType)) {
				linter.addMessage(source, "Null check always returns 'no'.", Message.Type.WARNING)
				BooleanLiteral(source, false)
			} else if(staticType !is OptionalType) {
				linter.addMessage(source, "Null check always returns 'yes'.", Message.Type.WARNING)
				BooleanLiteral(source, true)
			} else null
		}
	}
}
