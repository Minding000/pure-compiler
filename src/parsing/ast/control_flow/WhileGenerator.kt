package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.WhileGenerator
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.Element
import source_structure.Position
import util.indent

class WhileGenerator(start: Position, private val condition: Element, private val isPostCondition: Boolean): Element(start, condition.end) {

	override fun concretize(linter: Linter, scope: Scope): WhileGenerator {
		return WhileGenerator(this, condition.concretize(linter, scope), isPostCondition)
	}

	override fun toString(): String {
		return "WhileGenerator [${if(isPostCondition) "post" else "pre"}] {${"\n$condition".indent()}\n}"
	}
}