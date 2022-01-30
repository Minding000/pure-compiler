package parsing.ast.control_flow

import parsing.ast.Element
import parsing.ast.general.StatementBlock
import parsing.ast.literals.Identifier
import source_structure.Position
import util.indent

class OverGenerator(start: Position, val collection: Element, val keyDeclaration: Identifier?, val valueDeclaration: Identifier): Element(start, valueDeclaration.end) {

	override fun toString(): String {
		return "OverGenerator {${"\n$collection as ${if (keyDeclaration == null) "" else "$keyDeclaration, "}$valueDeclaration".indent()}\n}"
	}
}