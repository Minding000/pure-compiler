package components.tokenizer

interface WordDescriptor {
	fun includes(atom: WordAtom?): Boolean
}
