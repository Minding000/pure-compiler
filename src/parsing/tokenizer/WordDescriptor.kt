package parsing.tokenizer

interface WordDescriptor {
	fun includes(atom: WordAtom?): Boolean
}