package parsing.element_generator

import errors.user.UnexpectedWordError
import errors.user.UserError
import messages.Message
import parsing.syntax_tree.general.*
import source_structure.Project
import parsing.tokenizer.*
import java.util.*

class ElementGenerator(project: Project): Generator() {
	private val wordGenerator: WordGenerator
	override var currentWord: Word? = null
	override var nextWord: Word? = null
	override var parseForeignLanguageLiteralNext = false
	val logLevel = Message.Type.DEBUG
	val messages = LinkedList<Message>()
	val statementParser = StatementParser(this)
	val expressionParser = ExpressionParser(this)
	val typeParser = TypeParser(this)
	val literalParser = LiteralParser(this)

	init {
		wordGenerator = WordGenerator(project)
	}

	fun addMessage(description: String, type: Message.Type = Message.Type.INFO) {
		messages.add(Message(description, type))
	}

	fun printMessages() {
		val counts = Array(4) { 0 }
		for(message in messages) {
			counts[message.type.ordinal]++
			if(message.type >= logLevel)
				println("${message.type.name}: ${message.description}")
		}
		println("Total: "
				+ "${counts[Message.Type.ERROR.ordinal]} errors, "
				+ "${counts[Message.Type.WARNING.ordinal]} warnings, "
				+ "${counts[Message.Type.INFO.ordinal]} infos, "
				+ "${counts[Message.Type.DEBUG.ordinal]} debug messages"
				+ " (Log level: ${logLevel.name})")
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