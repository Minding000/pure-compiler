package parsing.ast.literals

import linter.Linter
import linter.elements.values.BooleanLiteral
import linter.scopes.Scope
import parsing.ast.general.ValueElement
import parsing.tokenizer.Word

class BooleanLiteral(word: Word): ValueElement(word) {

	override fun concretize(linter: Linter, scope: Scope): BooleanLiteral {
		return BooleanLiteral(this, getValue() == "yes")
	}

	override fun toString(): String {
		return "BooleanLiteral { ${getValue()} }"
	}
}