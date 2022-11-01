package parsing.syntax_tree.literals

import linting.Linter
import linting.semantic_model.values.VariableValue
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.ValueElement
import components.tokenizer.Word

open class Identifier(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): VariableValue {
		return VariableValue(this)
	}

	override fun toString(): String {
		return "Identifier { ${getValue()} }"
	}
}
