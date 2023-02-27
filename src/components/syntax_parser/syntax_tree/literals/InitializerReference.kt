package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.Word
import components.semantic_analysis.semantic_model.values.InitializerReference as SemanticInitializerReferenceModel

class InitializerReference(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticInitializerReferenceModel {
		return SemanticInitializerReferenceModel(this, scope)
	}

	override fun toString(): String {
		return "Init"
	}
}
