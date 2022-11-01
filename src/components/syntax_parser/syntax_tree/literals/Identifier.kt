package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.values.VariableValue
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.Word

open class Identifier(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): VariableValue {
		return VariableValue(this)
	}

	override fun toString(): String {
		return "Identifier { ${getValue()} }"
	}
}
