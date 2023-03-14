package components.syntax_parser.element_generator

import components.syntax_parser.syntax_tree.literals.*
import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.tokenizer.WordDescriptor
import source_structure.Position

class LiteralParser(private val elementGenerator: ElementGenerator): Generator() {
	override var currentWord: Word?
		get() = elementGenerator.currentWord
		set(value) { elementGenerator.currentWord = value }
	override var nextWord: Word?
		get() = elementGenerator.nextWord
		set(value) { elementGenerator.nextWord = value }
	override var parseForeignLanguageLiteralNext: Boolean
		get() = elementGenerator.parseForeignLanguageLiteralNext
		set(value) { elementGenerator.parseForeignLanguageLiteralNext = value }

	override fun getCurrentPosition(): Position = elementGenerator.getCurrentPosition()

	override fun consume(type: WordDescriptor): Word {
		return elementGenerator.consume(type)
	}

	/**
	 * Identifier:
	 *   <identifier>
	 */
	fun parseIdentifier(): Identifier {
		return Identifier(consume(WordAtom.IDENTIFIER))
	}

	/**
	 * NullLiteral:
	 *   null
	 */
	fun parseNullLiteral(): NullLiteral {
		return NullLiteral(consume(WordAtom.NULL_LITERAL))
	}

	/**
	 * BooleanLiteral:
	 *   <number>
	 */
	fun parseBooleanLiteral(): BooleanLiteral {
		return BooleanLiteral(consume(WordAtom.BOOLEAN_LITERAL))
	}

	/**
	 * NumberLiteral:
	 *   <number>
	 */
	fun parseNumberLiteral(): NumberLiteral {
		return NumberLiteral(consume(WordAtom.NUMBER_LITERAL))
	}

	/**
	 * StringLiteral:
	 *   <string>
	 */
	fun parseStringLiteral(): StringLiteral {
		return StringLiteral(consume(WordAtom.STRING_LITERAL))
	}
}
