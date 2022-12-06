package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.control_flow.OverGenerator as SemanticOverGeneratorModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.ValueElement
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position
import util.indent

class OverGenerator(start: Position, private val collection: ValueElement, private val keyDeclaration: Identifier?,
					private val valueDeclaration: Identifier?): Element(start, valueDeclaration?.end ?: collection.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticOverGeneratorModel {
		return SemanticOverGeneratorModel(this, collection.concretize(linter, scope),
			keyDeclaration?.let { keyDeclaration ->
				val variable = LocalVariableDeclaration(keyDeclaration)
				scope.declareValue(linter, variable)
				variable
			},
			valueDeclaration?.let { valueDeclaration ->
				val variable = LocalVariableDeclaration(valueDeclaration)
				scope.declareValue(linter, variable)
				variable
			})
	}

	override fun toString(): String {
		return "OverGenerator {${"\n$collection as ${if (keyDeclaration == null) "" else "$keyDeclaration, "}$valueDeclaration".indent()}\n}"
	}
}
