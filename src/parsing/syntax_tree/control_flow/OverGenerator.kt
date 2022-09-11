package parsing.syntax_tree.control_flow

import linting.Linter
import linting.semantic_model.control_flow.OverGenerator
import linting.semantic_model.values.VariableValueDeclaration
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.ValueElement
import parsing.syntax_tree.literals.Identifier
import source_structure.Position
import util.indent

class OverGenerator(start: Position, private val collection: ValueElement, private val keyDeclaration: Identifier?,
					private val valueDeclaration: Identifier?): Element(start, valueDeclaration?.end ?: collection.end) {

	override fun concretize(linter: Linter, scope: MutableScope): OverGenerator {
		return OverGenerator(this, collection.concretize(linter, scope),
			keyDeclaration?.let {
				val variable = VariableValueDeclaration(it)
				scope.declareValue(linter, variable)
				variable
			},
			valueDeclaration?.let {
				val variable = VariableValueDeclaration(it)
				scope.declareValue(linter, variable)
				variable
			})
	}

	override fun toString(): String {
		return "OverGenerator {${"\n$collection as ${if (keyDeclaration == null) "" else "$keyDeclaration, "}$valueDeclaration".indent()}\n}"
	}
}