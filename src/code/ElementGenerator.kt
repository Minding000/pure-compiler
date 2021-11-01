package code

import elements.Identifier
import elements.Program
import elements.literals.NumberLiteral
import elements.literals.StringLiteral
import elements.operations.*
import errors.SyntaxError
import objects.Element
import objects.Project
import objects.Word
import objects.WordType
import util.stringify
import java.lang.Exception
import java.util.*

class ElementGenerator(project: Project) {
    private val wordGenerator: WordGenerator
    private var currentWord: Word?
    private var nextWord: Word?

    init {
        wordGenerator = WordGenerator(project)
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
        throw SyntaxError("Unexpected ${word.type} in ${word.line.file.name}:${word.line.number}:${word.start}: '${word.value.stringify()}'.\nExpected $expectation instead.")
    }

    /**
     * Program:
     *   <empty>
     *   <Program>\n
     *   <Statement>
     *   <Program>\n<Statement>
     */
    fun parseProgram(): Program {
        val statements = LinkedList<Element>()
        while(currentWord != null) {
            while(currentWord?.type == WordType.NEW_LINE)
                consume(WordType.NEW_LINE)
            if(currentWord != null)
                statements.add(parseStatement())
        }
        return Program(statements)
    }

    /**
     * Statement:
     *   <Print>
     *   <Declaration>
     *   <Assignment>
     *   <Expression>
     */
    private fun parseStatement(): Element {
        if(currentWord?.type == WordType.ECHO)
            return parsePrint()
        if(currentWord?.type == WordType.VAR)
            return parseDeclaration()
        if(nextWord?.type == WordType.ASSIGNMENT)
            return parseAssignment()
        return parseExpression()
    }


    /**
     * Print:
     *   echo <Identifier>[,<Identifier>]...
     */
    private fun parsePrint(): Print {
        consume(WordType.ECHO)
        val identifiers = LinkedList<Identifier>()
        identifiers.add(parseIdentifier())
        while(currentWord?.type == WordType.COMMA) {
            consume(WordType.COMMA)
            identifiers.add(parseIdentifier())
        }
        return Print(identifiers)
    }

    /**
     * Declaration:
     *   var <DeclarationPart>[,<DeclarationPart>]...
     */
    private fun parseDeclaration(): Declaration {
        consume(WordType.VAR)
        val declarationParts = LinkedList<Element>()
        declarationParts.add(parseDeclarationPart())
        while(currentWord?.type == WordType.COMMA) {
            consume(WordType.COMMA)
            declarationParts.add(parseDeclarationPart())
        }
        return Declaration(declarationParts)
    }

    /**
     * DeclarationPart:
     *   <Identifier>
     *   <Assignment>
     */
    private fun parseDeclarationPart(): Element {
        if(nextWord?.type == WordType.ASSIGNMENT)
            return parseAssignment()
        return parseIdentifier()
    }

    /**
     * Assignment:
     *   <Identifier> = <Expression>
     */
    private fun parseAssignment(): Assignment {
        val identifier = parseIdentifier()
        consume(WordType.ASSIGNMENT)
        return Assignment(identifier, parseExpression())
    }

    /**
     * Expression:
     *   <Addition>
     */
    private fun parseExpression(): Element {
        return parseAddition()
    }

    /**
     * Addition:
     *   <Multiplication>
     *   <Addition> + <Atom>
     *   <Addition> - <Atom>
     */
    private fun parseAddition(): Element {
        var addition: Element = parseMultiplication()
        while(currentWord?.type == WordType.ADDITION) {
            val operator = consume(WordType.ADDITION)
            addition = Addition(addition, parseMultiplication(), operator == "-")
        }
        return addition
    }

    /**
     * Multiplication:
     *   <Atom>
     *   <Multiplication> * <Atom>
     *   <Multiplication> / <Atom>
     */
    private fun parseMultiplication(): Element {
        var multiplication: Element = parsePrimary()
        while(currentWord?.type == WordType.MULTIPLICATION) {
            val operator = consume(WordType.MULTIPLICATION)
            multiplication = Multiplication(multiplication, parsePrimary(), operator == "/")
        }
        return multiplication
    }

    /**
     * Primary:
     *   <Atom>
     *   (<Expression>)
     */
    private fun parsePrimary(): Element {
        if(currentWord?.type == WordType.PARENTHESES_OPEN) {
            consume(WordType.PARENTHESES_OPEN)
            val expression = parseExpression()
            consume(WordType.PARENTHESES_CLOSE)
            return expression
        }
        return parseAtom()
    }

    /**
     * Atom:
     *   <NumberLiteral>
     *   <StringLiteral>
     *   <Identifier>
     */
    private fun parseAtom(): Element {
        val word = checkEndOfFile("atom")
        return when(word.type) {
            WordType.NUMBER_LITERAL -> parseNumberLiteral()
            WordType.STRING_LITERAL -> parseStringLiteral()
            WordType.IDENTIFIER -> parseIdentifier()
            else -> onUnexpectedWord(word, "atom")
        }
    }

    /**
     * Identifier:
     *   <identifier>
     */
    private fun parseIdentifier(): Identifier {
        return Identifier(consume(WordType.IDENTIFIER))
    }

    /**
     * NumberLiteral:
     *   <number>
     */
    private fun parseNumberLiteral(): NumberLiteral {
        val value = consume(WordType.NUMBER_LITERAL)
        return NumberLiteral(Integer.parseInt(value))
    }

    /**
     * StringLiteral:
     *   <number>
     */
    private fun parseStringLiteral(): StringLiteral {
        val value = consume(WordType.STRING_LITERAL)
        return StringLiteral(value.substring(1, value.length - 1))
    }
}