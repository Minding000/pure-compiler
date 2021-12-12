package errors.user

import parsing.tokenizer.Word
import parsing.tokenizer.WordType
import util.stringify

/**
 * Represents an unexpected word being encountered.
 * This error is the programmers fault.
 */
class UnexpectedWordError(message: String): SyntaxError(message) {

	constructor(word: Word, expectation: String): this("Unexpected ${word.type} in ${word.getStartString()}: '${word.getValue().stringify()}'.\nExpected $expectation instead.")
	constructor(word: Word, expectation: WordType): this(word, expectation.toString())
	constructor(word: Word, expectation: List<WordType>): this(word, expectation.toString())

}