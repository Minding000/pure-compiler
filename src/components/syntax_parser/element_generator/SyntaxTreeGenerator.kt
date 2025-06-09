package components.syntax_parser.element_generator

import components.syntax_parser.syntax_tree.general.File
import components.syntax_parser.syntax_tree.general.Program
import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.tokenizer.WordDescriptor
import components.tokenizer.WordGenerator
import errors.user.UnexpectedWordError
import logger.Issue
import source_structure.Position
import source_structure.Project
import java.util.*

class SyntaxTreeGenerator(val project: Project): Generator() {
	val wordGenerator = WordGenerator(project)
	override var currentWord: Word? = null
	override var nextWord: Word? = null
	override var parseForeignLanguageLiteralNext = false
	val statementParser = StatementParser(this)
	val expressionParser = ExpressionParser(this)
	val typeParser = TypeParser(this)
	val literalParser = LiteralParser(this)

	fun addIssue(issue: Issue) = project.context.logger.add(issue)

	override fun getCurrentPosition(): Position = wordGenerator.getCurrentPosition()

	//TODO add parser error suggestions e.g. unexpected 'WHILE'. Did you forget to precede it with 'loop'?
	//TODO add missing brace detection based on indentation e.g. <above all errors for this file:> It seems like you are missing a brace at 11:2.
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
		nextWord = wordGenerator.getNextWord()
		return consumedWord
	}

	fun skipLine(invalidWord: Word) {
		wordGenerator.skipLine(invalidWord)
		currentWord = wordGenerator.getNextWord()
		nextWord = wordGenerator.getNextWord()
	}

	fun parseProgram(): Program {
		project.context.logger.addPhase("Parsing program")
		val files = LinkedList<File>()
		while(!wordGenerator.done) {
			currentWord = wordGenerator.getNextWord()
			nextWord = wordGenerator.getNextWord()
			files.add(parseFile())
			wordGenerator.loadNextFile()
		}
		return Program(files)
	}

	/**
	 * File:
	 *   [<Statement>]...
	 */
	private fun parseFile(): File {
		val file = wordGenerator.getFile()
		val statements = statementParser.parseStatements()
		return File(file, statements)
	}
}
