package parsing.tokenizer

import errors.user.SyntaxError
import source_structure.*
import util.stringify
import java.util.regex.Pattern

class WordGenerator(private val project: Project) {
	private var moduleIndex = -1
	private var fileIndex = -1
	private var lineIndex = -1
	private var characterIndex = -1
	private lateinit var module: Module
	private lateinit var file: File
	private lateinit var line: Line
	private var position = -1

	init {
		nextModule()
	}

	private fun nextModule(): Boolean {
		moduleIndex++
		val modules = project.modules
		return if(moduleIndex < modules.size) {
			module = modules[moduleIndex]
			fileIndex = -1
			nextFile()
		} else {
			false
		}
	}

	private fun nextFile(): Boolean {
		fileIndex++
		val files = module.files
		return if(fileIndex < files.size) {
			file = files[fileIndex]
			lineIndex = -1
			nextLine()
			position = 0
			true
		} else {
			nextModule()
		}
	}

	private fun nextLine() {
		lineIndex++
		line = file.lines[lineIndex]
		characterIndex = 0
	}

	fun getRemainingLine(): Word? {
		if(file.content.length == position)
			return null
		val input = file.content.substring(position)
		var lineEnd = input.indexOf('\n')
		if(lineEnd == 0)
			return null
		if(lineEnd == -1)
			lineEnd = input.length
		val remainingLine = input.substring(0, lineEnd)
		val wordStartIndex = position
		position += remainingLine.length
		val wordStartColumn = characterIndex
		characterIndex += remainingLine.length
		return Word(Position(wordStartIndex, line, wordStartColumn), Position(position, line, characterIndex), WordAtom.FOREIGN_LANGUAGE)
	}

	fun getNextWord(): Word? {
		while(file.content.length == position) {
			if(!nextFile())
				return null
		}
		val input = file.content.substring(position)
		val matcher = Pattern
			.compile("")
			.matcher(input)
		for(wordType in WordAtom.values()) {
			matcher.usePattern(wordType.pattern)
			if(matcher.find()) {
				val rawWord = input.substring(0, matcher.end())
				val wordStartIndex = position
				position += rawWord.length
				val wordStartColumn = characterIndex
				characterIndex += rawWord.length
				val word = if (wordType.ignore) getNextWord() else Word(Position(wordStartIndex, line, wordStartColumn), Position(position, line, characterIndex), wordType)
				// Increment line and character indices
				if(wordType == WordAtom.LINE_BREAK) {
					nextLine()
				} else if(wordType.isMultiline) {
					for(i in 0 until rawWord.count { c -> c == '\n' })
						nextLine()
					characterIndex = rawWord.length - (rawWord.lastIndexOf('\n') + 1)
				}
				return word
			}
		}
		throw SyntaxError("Unknown word in ${file.name}:${line.number}:$characterIndex: '${input.first().stringify()}'")
	}
}