package parsing.syntax_tree.control_flow

import linting.Linter
import linting.semantic_model.control_flow.YieldStatement
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.ValueElement
import source_structure.Position

class YieldStatement(start: Position, private val key: Element?, private val value: Element): ValueElement(start, value.end) {

	override fun concretize(linter: Linter, scope: MutableScope): YieldStatement {
		return YieldStatement(this, key?.concretize(linter, scope), value.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Yield { ${if(key == null) "" else "$key "}$value }"
	}
}