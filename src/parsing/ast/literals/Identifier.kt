package parsing.ast.literals

import linter.Linter
import linter.elements.values.VariableValue
import linter.scopes.MutableScope
import parsing.ast.general.ValueElement
import parsing.tokenizer.Word

open class Identifier(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: MutableScope): VariableValue {
		return VariableValue(this)
	}

	override fun toString(): String {
		return "Identifier { ${getValue()} }"
	}
}