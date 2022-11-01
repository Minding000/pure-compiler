package parsing.element_generator

import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.tokenizer.WordDescriptor
import components.tokenizer.WordGenerator
import errors.user.UnexpectedWordError
import errors.user.UserError
import messages.Message
import messages.MessageLogger
import parsing.syntax_tree.general.*
import source_structure.Project
import java.util.*

class ElementGenerator(project: Project): Generator() {
	val wordGenerator = WordGenerator(project)
	override var currentWord: Word? = null
	override var nextWord: Word? = null
	override var parseForeignLanguageLiteralNext = false
	val logger = MessageLogger("parser", Message.Type.INFO)
	val statementParser = StatementParser(this)
	val expressionParser = ExpressionParser(this)
	val typeParser = TypeParser(this)
	val literalParser = LiteralParser(this)

	fun addMessage(description: String, type: Message.Type = Message.Type.INFO) {
		logger.add(Message(description, type))
	}

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

	fun parseProgram(): Program {
		logger.addPhase("Parsing program")
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
			try {
				statements.add(statementParser.parseStatement())
			} catch(error: UserError) {
				addMessage(error.message, Message.Type.ERROR)
				currentWord?.let { invalidWord ->
					wordGenerator.skipLine(invalidWord)
					currentWord = wordGenerator.getNextWord()
					nextWord = wordGenerator.getNextWord()
				}
			}
		}
		return File(file.getStart(), file.getEnd(), file, statements)
	}
}
