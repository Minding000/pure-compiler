package components.syntax_parser.element_generator

import components.syntax_parser.syntax_tree.literals.*
import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.tokenizer.WordDescriptor
import source_structure.Position

class LiteralParser(private val syntaxTreeGenerator: SyntaxTreeGenerator): Generator() {
	override val currentWord: Word?
		get() = syntaxTreeGenerator.currentWord
	override val nextWord: Word?
		get() = syntaxTreeGenerator.nextWord
	override var parseForeignLanguageLiteralNext: Boolean
		get() = syntaxTreeGenerator.parseForeignLanguageLiteralNext
		set(value) {
			syntaxTreeGenerator.parseForeignLanguageLiteralNext = value
		}

	override fun getCurrentPosition(): Position = syntaxTreeGenerator.getCurrentPosition()

	override fun consume(type: WordDescriptor): Word {
		return syntaxTreeGenerator.consume(type)
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
	 *   <boolean>
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
		val word = consume(WordAtom.STRING_LITERAL)
		val value = word.getValue()
		return StringLiteral(word, value.substring(1, value.length - 1))
	}
}
