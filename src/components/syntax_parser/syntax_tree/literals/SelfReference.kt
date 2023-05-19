package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.Word
import source_structure.Position
import components.semantic_analysis.semantic_model.values.SelfReference as SemanticSelfReferenceModel

class SelfReference(word: Word, private val specifier: ObjectType?, end: Position): ValueElement(word.start, end) {

	override fun toSemanticModel(scope: MutableScope): SemanticSelfReferenceModel {
		return SemanticSelfReferenceModel(this, scope, specifier?.toSemanticModel(scope))
	}

	override fun toString(): String {
		var stringRepresentation = "This"
		if(specifier != null)
			stringRepresentation += " [ $specifier ]"
		return stringRepresentation
	}
}
