package components.tokenizer

import errors.user.SyntaxError
import source_structure.*
import util.stringify
import java.util.regex.Pattern

class WordGenerator(private val project: Project) {
	private val matcher = Pattern.compile("").matcher("")
	private var moduleIndex = -1
	private var fileIndex = -1
	private var lineIndex = -1
	private var characterIndex = -1
	private lateinit var module: Module
	private lateinit var file: File
	private lateinit var line: Line
	private var position = -1
	var done = false

	init {
		loadNextModule()
	}

	private fun loadNextModule() {
		moduleIndex++
		val modules = project.modules
		if(moduleIndex >= modules.size) {
			done = true
			return
		}
		module = modules[moduleIndex]
		fileIndex = -1
		loadNextFile()
	}

	fun loadNextFile() {
		fileIndex++
		val files = module.files
		if(fileIndex >= files.size) {
			loadNextModule()
			return
		}
		file = files[fileIndex]
		matcher.reset(file.content)
		lineIndex = -1
		nextLine()
		position = 0
	}

	fun getFile(): File {
		return file
	}

	private fun nextLine(count: Int = 1) {
		lineIndex += count
		line = file.lines[lineIndex]
		characterIndex = 0
	}

	fun getRemainingLine(wordType: WordAtom): Word? {
		if(file.content.length == position)
			return null
		val input = file.content.substring(position)
		var remainingCharacterCount = input.indexOf('\n')
		if(remainingCharacterCount == 0)
			return null
		if(remainingCharacterCount == -1)
			remainingCharacterCount = input.length
		val wordStartIndex = position
		position += remainingCharacterCount
		val wordStartColumn = characterIndex
		characterIndex += remainingCharacterCount
		return Word(Position(wordStartIndex, line, wordStartColumn), Position(position, line, characterIndex), wordType)
	}

	fun skipLine(referenceWord: Word) {
		line = referenceWord.end.line
		lineIndex = line.number - 1
		position = line.end
		characterIndex = line.getLength()
	}

	fun scanForCharacters(start: Position, pattern: Pattern): Char? {
		matcher.region(start.index, file.content.length).usePattern(pattern)
		if(!matcher.find())
			return null
		return file.content[matcher.start()]
	}

	fun getNextWord(): Word? {
		if(file.content.length == position)
			return null
		matcher.region(position, file.content.length)
		for(wordType in WordAtom.values()) {
			matcher.usePattern(wordType.pattern)
			if(!matcher.lookingAt())
				continue
			val rawWord = file.content.substring(matcher.start(), matcher.end())
			val wordStartIndex = position
			position += rawWord.length
			val wordStartColumn = characterIndex
			characterIndex += rawWord.length
			val word = if(wordType.ignore)
				getNextWord()
			else
				Word(Position(wordStartIndex, line, wordStartColumn),
					Position(position, line, characterIndex), wordType)
			// Increment line and character indices
			if(wordType == WordAtom.LINE_BREAK) {
				nextLine()
			} else if(wordType.isMultiline) {
				val containedLineBreaks = rawWord.count { c -> c == '\n' }
				if(containedLineBreaks > 0) {
					nextLine(containedLineBreaks)
					characterIndex = rawWord.length - (rawWord.lastIndexOf('\n') + 1)
				}
			}
			return word
		}
		throw SyntaxError("Unknown word in ${file.name}:${line.number}:$characterIndex: " +
			"'${file.content[position].stringify()}'")
	}
}
