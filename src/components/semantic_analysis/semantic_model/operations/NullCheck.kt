package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.Value
import messages.Message
import components.syntax_parser.syntax_tree.operations.NullCheck as NullCheckSyntaxTree

class NullCheck(override val source: NullCheckSyntaxTree, val value: Value): Value(source) {

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
