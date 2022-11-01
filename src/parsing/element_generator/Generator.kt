package parsing.element_generator

import errors.user.UnexpectedEndOfFileError
import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.tokenizer.WordDescriptor

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
		return currentWord ?: throw UnexpectedEndOfFileError(expectation)
	}

	internal fun getCurrentWord(expectation: WordDescriptor): Word {
		return getCurrentWord(expectation.toString())
	}
}
