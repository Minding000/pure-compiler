package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.Word
import source_structure.Position
import components.semantic_analysis.semantic_model.values.SuperReference as SemanticSuperReferenceModel

class SuperReference(word: Word, private val specifier: ObjectType?, end: Position): ValueElement(word.start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticSuperReferenceModel {
		return SemanticSuperReferenceModel(this, scope, specifier?.concretize(linter, scope))
	}

	override fun toString(): String {
		var stringRepresentation = "Super"
		if(specifier != null)
			stringRepresentation += " [ $specifier ]"
		return stringRepresentation
	}
}
