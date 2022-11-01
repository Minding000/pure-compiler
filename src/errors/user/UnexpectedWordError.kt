package errors.user

import components.tokenizer.Word
import components.tokenizer.WordDescriptor
import util.stringify

/**
 * Represents an unexpected word being encountered.
 * This error is the programmers fault.
 */
class UnexpectedWordError(message: String): SyntaxError(message) {

	constructor(word: Word, expectation: String): this("Unexpected ${word.type} in ${word.getStartString()}: '${word.getValue().stringify()}'.\n${word.getHighlight()}\nExpected $expectation instead.")
	constructor(word: Word, expectation: WordDescriptor): this(word, expectation.toString())
}
