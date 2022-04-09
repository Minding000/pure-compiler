package parsing.element_generator

import errors.user.UnexpectedWordError
import parsing.ast.general.*
import source_structure.Project
import parsing.tokenizer.*
import java.util.*

class ElementGenerator(project: Project): Generator() {
	private val wordGenerator: WordGenerator
	override var currentWord: Word? = null
	override var nextWord: Word? = null
	override var parseForeignLanguageLiteralNext = false
	val statementParser = StatementParser(this)
	val expressionParser = ExpressionParser(this)
	val typeParser = TypeParser(this)
	val literalParser = LiteralParser(this)

	init {
		wordGenerator = WordGenerator(project)
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

	fun parseProgram(): Program {
		val files = LinkedList<File>()
		while(!wordGenerator.done) {
			wordGenerator.loadNextFile()
			currentWord = wordGenerator.getNextWord()
			nextWord = wordGenerator.getNextWord()
			files.add(parseFile())
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
		val statements = LinkedList<Element>()
		while(currentWord != null) {
			consumeLineBreaks()
			if(currentWord == null)
				break
			statements.add(statementParser.parseStatement())
		}
		return File(file.getStart(), file.getEnd(), file, statements)
	}
}