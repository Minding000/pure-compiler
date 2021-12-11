package elements.identifier

import elements.ValueElement
import errors.internal.CompilerError
import scopes.Scope
import word_generation.Word

abstract class IdentifierReference<T: Identifier>(val scope: Scope, word: Word): ValueElement(word.start, word.end) {
	open var target: T? = null

	fun requireTarget(): T {
		return target ?: throw CompilerError("Identifier reference is not referencing identifier.")
	}

	abstract fun resolve()
}