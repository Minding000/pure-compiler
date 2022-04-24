package parsing.ast.operations

import linter.Linter
import linter.elements.general.Unit
import linter.elements.operations.UnaryModification
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.tokenizer.Word
import parsing.tokenizer.WordAtom

class UnaryModification(val target: Element, val operator: Word): Element(target.start, operator.end) {

	override fun concretize(linter: Linter, scope: Scope): Unit {
		return UnaryModification(this, target.concretize(linter, scope), operator.type == WordAtom.INCREMENT)
	}

	override fun toString(): String {
		return "UnaryModification { $target${operator.getValue()} }"
	}
}