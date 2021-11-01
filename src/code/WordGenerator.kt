package code

import errors.SyntaxError
import objects.*
import util.stringify
import java.util.regex.Pattern

class WordGenerator(private val project: Project) {
    private var fileIndex = -1
    private var lineIndex = -1
    private var characterIndex = -1
    private lateinit var file: File
    private lateinit var line: Line
    private var position = -1

    init {
        loadNextFile()
    }

    private fun loadNextFile(): Boolean {
        fileIndex++
        if(fileIndex < project.files.size) {
            file = project.files[fileIndex]
            lineIndex = -1
            nextLine()
            position = 0
            return true
        }
        return false
    }

    private fun nextLine() {
        lineIndex++
        line = file.lines[lineIndex]
        characterIndex = 0
    }

    fun getNextWord(): Word? {
        val input = file.content.substring(position)
        if (input.isEmpty())
            return null
        val matcher = Pattern
            .compile("")
            .matcher(input)
        for (wordType in WordType.values()) {
            matcher.usePattern(wordType.pattern)
            if (matcher.find()) {
                val rawWord = input.substring(0, matcher.end())
                position += rawWord.length
                val wordStart = characterIndex;
                characterIndex += rawWord.length
                val word = if (wordType.ignore) getNextWord() else Word(line, wordStart, wordType, rawWord)
                // Increment line and character indices
                if(wordType == WordType.NEW_LINE) {
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