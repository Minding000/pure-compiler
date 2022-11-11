package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.values.SelfReference as SemanticSelfReferenceModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.Word

class SelfReference(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticSelfReferenceModel {
		return SemanticSelfReferenceModel(this)
	}

	override fun toString(): String {
		return "This"
	}
}
