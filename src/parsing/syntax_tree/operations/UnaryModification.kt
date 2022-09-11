package parsing.syntax_tree.operations

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.operations.UnaryModification
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import parsing.tokenizer.Word
import parsing.tokenizer.WordAtom

class UnaryModification(val target: Element, val operator: Word): Element(target.start, operator.end) {

	override fun concretize(linter: Linter, scope: MutableScope): Unit {
		return UnaryModification(this, target.concretize(linter, scope), operator.type == WordAtom.INCREMENT)
	}

	override fun toString(): String {
		return "UnaryModification { $target${operator.getValue()} }"
	}
}