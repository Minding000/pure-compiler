package parsing.syntax_tree.control_flow

import linting.Linter
import linting.semantic_model.control_flow.WhileGenerator
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import source_structure.Position
import util.indent

class WhileGenerator(start: Position, private val condition: Element, private val isPostCondition: Boolean): Element(start, condition.end) {

	override fun concretize(linter: Linter, scope: MutableScope): WhileGenerator {
		return WhileGenerator(this, condition.concretize(linter, scope), isPostCondition)
	}

	override fun toString(): String {
		return "WhileGenerator [${if(isPostCondition) "post" else "pre"}] {${"\n$condition".indent()}\n}"
	}
}