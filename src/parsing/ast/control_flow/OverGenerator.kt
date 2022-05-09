package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.OverGenerator
import linter.elements.values.LocalVariableDeclaration
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.literals.Identifier
import source_structure.Position
import util.indent

class OverGenerator(start: Position, private val collection: Element, private val keyDeclaration: Identifier?,
					private val valueDeclaration: Identifier?): Element(start, valueDeclaration?.end ?: collection.end) {

	override fun concretize(linter: Linter, scope: Scope): OverGenerator {
		return OverGenerator(this, collection.concretize(linter, scope),
			keyDeclaration?.let {
				val variable = LocalVariableDeclaration(it)
				scope.declareValue(linter, variable)
				variable
			},
			valueDeclaration?.let {
				val variable = LocalVariableDeclaration(it)
				scope.declareValue(linter, variable)
				variable
			})
	}

	override fun toString(): String {
		return "OverGenerator {${"\n$collection as ${if (keyDeclaration == null) "" else "$keyDeclaration, "}$valueDeclaration".indent()}\n}"
	}
}