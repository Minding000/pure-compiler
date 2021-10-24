package code

import elements.Program
import elements.literals.NumberLiteral
import elements.literals.StringLiteral
import errors.SyntaxError
import objects.Element
import objects.Word
import objects.WordType
import util.stringify
import java.lang.Exception
import java.util.*

class ElementGenerator(sourceCode: String) {
    private val wordGenerator: WordGenerator
    private var currentWord: Word?
    private var nextWord: Word?

    init {
        wordGenerator = WordGenerator(sourceCode)
        currentWord = wordGenerator.getNextWord()
        nextWord = wordGenerator.getNextWord()
    }

    private fun consume(type: WordType): String {
        val consumedWord = checkEndOfFile(type)
        if(consumedWord.type != type)
            onUnexpectedWord(consumedWord, type)
        currentWord = nextWord
        nextWord = wordGenerator.getNextWord()
        return consumedWord.value
    }

    private fun checkEndOfFile(expectation: WordType): Word {
        return checkEndOfFile(expectation.toString())
    }

    private fun checkEndOfFile(expectation: String): Word {
        return currentWord ?: throw Exception("Unexpected end of file.\nExpected $expectation instead.")
    }

    private fun onUnexpectedWord(word: Word, expectation: WordType): Element {
        return onUnexpectedWord(word, expectation.toString())
    }

    private fun onUnexpectedWord(word: Word, expectation: String): Element {
        throw SyntaxError("Unexpected ${word.type}: ${word.value.stringify()}.\nExpected $expectation instead.")
    }

    /**
     * Program:
     *   <empty>
     *   <Statement>
     *   <Statement>\n<Program>
     */
    fun parseProgram(): Program {
        val statements = LinkedList<Element>()
        while(currentWord != null) {
            statements.add(parseStatement())
            if(currentWord?.type == WordType.NEW_LINE)
                consume(WordType.NEW_LINE)
        }
        return Program(statements)
    }

    /**
     * Statement:
     *   <NumberLiteral>
     *   <StringLiteral>
     */
    private fun parseStatement(): Element {
        val word = checkEndOfFile("statement")
        return when(word.type) {
            WordType.NUMBER_LITERAL -> parseNumberLiteral()
            WordType.STRING_LITERAL -> parseStringLiteral()
            else -> onUnexpectedWord(word, "statement")
        }
    }

    /**
     * NumberLiteral:
     *   <number>
     */
    private fun parseNumberLiteral(): Element {
        val value = consume(WordType.NUMBER_LITERAL)
        return NumberLiteral(Integer.parseInt(value))
    }

    /**
     * StringLiteral:
     *   <number>
     */
    private fun parseStringLiteral(): Element {
        val value = consume(WordType.STRING_LITERAL)
        return StringLiteral(value.substring(1, value.length - 1))
    }
}