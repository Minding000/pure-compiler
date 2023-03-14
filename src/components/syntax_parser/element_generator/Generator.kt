package components.syntax_parser.element_generator

import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.tokenizer.WordDescriptor
import errors.user.UnexpectedEndOfFileError
import source_structure.Position
import source_structure.Section

abstract class Generator {
	internal abstract var currentWord: Word?
	internal abstract var nextWord: Word?
	internal abstract var parseForeignLanguageLiteralNext: Boolean

	internal abstract fun consume(type: WordDescriptor): Word

	internal fun consumeLineBreaks() {
		while(currentWord?.type == WordAtom.LINE_BREAK)
			consume(WordAtom.LINE_BREAK)
	}

	internal fun getCurrentWord(expectation: String): Word {
		val currentWord = currentWord
		if(currentWord == null) {
			val position = getCurrentPosition()
			throw UnexpectedEndOfFileError(expectation, Section(position, position))
		}
		return currentWord
	}

	internal fun getCurrentWord(expectation: WordDescriptor): Word {
		return getCurrentWord(expectation.toString())
	}

	internal abstract fun getCurrentPosition(): Position
}
