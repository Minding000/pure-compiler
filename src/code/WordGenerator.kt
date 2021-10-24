package code

import errors.SyntaxError
import objects.WordType
import java.util.HashMap
import objects.Word
import util.stringify
import java.util.regex.Pattern

class WordGenerator(private val sourceCode: String) {
    private var position = 0
    private val wordTypes: MutableMap<Pattern, WordType?> = HashMap()

    init {
        // Whitespace
        declare("\\s", null)
        declare("\n", WordType.NEW_LINE)
        // Binary operators
        //declare("\\+", WordType.ADD)
        // Literals
        declare("\".*?\"", WordType.STRING_LITERAL)
        declare("\\d+", WordType.NUMBER_LITERAL)
        // Keywords
        declare("var\\b", WordType.VAR)
        // Identifier
    }

    private fun declare(regex: String, type: WordType?) {
        wordTypes[Pattern.compile("^${regex}")] = type
    }

    fun getNextWord(): Word? {
        val input = sourceCode.substring(position)
        if (input.isEmpty())
            return null
        val matcher = Pattern
            .compile("")
            .matcher(input)
        for (wordType in wordTypes) {
            matcher.usePattern(wordType.key)
            if (matcher.find()) {
                val rawWord = input.substring(0, matcher.end())
                position += rawWord.length
                val type = wordType.value
                // Ignore null word types
                return if (type == null) getNextWord() else Word(type, rawWord)
            }
        }
        throw SyntaxError("Unknown word: '${input.first().stringify()}'")
    }
}