package parsing.element_generator

import parsing.ast.*
import parsing.ast.operations.*
import errors.user.UnexpectedWordError
import parsing.ast.access.Index
import parsing.ast.access.MemberAccess
import parsing.ast.control_flow.*
import parsing.ast.definitions.*
import parsing.ast.general.*
import source_structure.Project
import parsing.ast.literals.*
import parsing.tokenizer.*
import java.util.*

class ElementGenerator(project: Project): Generator() {
	private val wordGenerator: WordGenerator
	override var currentWord: Word? = null
	override var nextWord: Word? = null
	override var parseForeignLanguageLiteralNext = false
	private val statementParser = StatementParser(this)
	val expressionParser = ExpressionParser(this)
	val typeParser = TypeParser(this)
	val literalParser = LiteralParser(this)

	init {
		wordGenerator = WordGenerator(project)
		currentWord = wordGenerator.getNextWord()
		nextWord = wordGenerator.getNextWord()
	}

	override fun consume(type: WordDescriptor): Word {
		val consumedWord = getCurrentWord(type)
		if(!type.includes(consumedWord.type))
			throw UnexpectedWordError(consumedWord, type)
		currentWord = nextWord
		if(parseForeignLanguageLiteralNext) {
			parseForeignLanguageLiteralNext = false
			val foreignLanguage = wordGenerator.getRemainingLine()
			if(foreignLanguage != null) {
				nextWord = foreignLanguage
				return consumedWord
			}
		}
		nextWord = wordGenerator.getNextWord()
		return consumedWord
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
			consumeLineBreaks()
			if(currentWord == null)
				break
			statements.add(statementParser.parseStatement())
		}
		return Program(statements)
	}
}