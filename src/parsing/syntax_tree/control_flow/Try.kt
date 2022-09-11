package parsing.syntax_tree.control_flow

import linting.Linter
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import source_structure.Position
import linting.semantic_model.control_flow.Try
import parsing.syntax_tree.general.ValueElement

class Try(private val expression: Element, private val isOptional: Boolean, start: Position): ValueElement(start, expression.end) {

	override fun concretize(linter: Linter, scope: MutableScope): Try {
		return Try(this, expression.concretize(linter, scope), isOptional)
	}

	override fun toString(): String {
		return "Try [ ${if(isOptional) "null" else "uncheck"} ] { $expression }"
	}
}