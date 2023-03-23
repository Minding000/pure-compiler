package components.syntax_parser.element_generator

import components.syntax_parser.syntax_tree.general.File
import components.syntax_parser.syntax_tree.general.Program
import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.tokenizer.WordDescriptor
import components.tokenizer.WordGenerator
import errors.user.UnexpectedWordError
import logger.Issue
import logger.Logger
import logger.Severity
import source_structure.Position
import source_structure.Project
import java.util.*

class ElementGenerator(project: Project): Generator() {
	val wordGenerator = WordGenerator(project)
	override var currentWord: Word? = null
	override var nextWord: Word? = null
	override var parseForeignLanguageLiteralNext = false
	val logger = Logger("parser", Severity.INFO)
	val statementParser = StatementParser(this)
	val expressionParser = ExpressionParser(this)
	val typeParser = TypeParser(this)
	val literalParser = LiteralParser(this)

	fun addIssue(issue: Issue) = logger.add(issue)

	override fun getCurrentPosition(): Position = wordGenerator.getCurrentPosition()

	override fun consume(type: WordDescriptor): Word {
		val consumedWord = getCurrentWord(type)
		if(!type.includes(consumedWord.type))
			throw UnexpectedWordError(consumedWord, type)
		currentWord = nextWord
		if(parseForeignLanguageLiteralNext) {
			parseForeignLanguageLiteralNext = false
			val foreignLanguage = wordGenerator.getRemainingLine(WordAtom.FOREIGN_LANGUAGE)
			if(foreignLanguage != null) {
				nextWord = foreignLanguage
				return consumedWord
			}
		}
		nextWord = wordGenerator.getNextWord(logger)
		return consumedWord
	}

	fun skipLine(invalidWord: Word) {
		wordGenerator.skipLine(invalidWord)
		currentWord = wordGenerator.getNextWord(logger)
		nextWord = wordGenerator.getNextWord(logger)
	}

	fun parseProgram(): Program {
		logger.addPhase("Parsing program")
		val files = LinkedList<File>()
		while(!wordGenerator.done) {
			currentWord = wordGenerator.getNextWord(logger)
			nextWord = wordGenerator.getNextWord(logger)
			files.add(parseFile())
			wordGenerator.loadNextFile()
		}
		return Program(files)
	}

	/**
	 * File:
	 *   <empty>
	 *   <Program>\n
	 *   <Statement>
	 *   <Program>\n<Statement>
	 */
	private fun parseFile(): File {
		val file = wordGenerator.getFile()
		val statements = statementParser.parseStatements()
		return File(file, statements)
	}
}
