package parsing.tokenizer

enum class WordType(vararg val atoms: WordAtom): WordDescriptor {
	UNARY_OPERATOR(
		WordAtom.NOT,
		WordAtom.PLUS,
		WordAtom.MINUS),
	UNARY_MODIFICATION(
		WordAtom.INCREMENT,
		WordAtom.DECREMENT),
	BINARY_MODIFICATION(
		WordAtom.ADD,
		WordAtom.SUBTRACT,
		WordAtom.MULTIPLY,
		WordAtom.DIVIDE),
	BINARY_BOOLEAN_OPERATOR(
		WordAtom.AND,
		WordAtom.OR),
	ADDITION(
		WordAtom.PLUS,
		WordAtom.MINUS),
	MULTIPLICATION(
		WordAtom.STAR,
		WordAtom.SLASH),
	EQUALITY(
		WordAtom.EQUALS,
		WordAtom.NOT_EQUALS),
	COMPARISON(
		WordAtom.GREATER_OR_EQUALS,
		WordAtom.LOWER_OR_EQUALS,
		WordAtom.GREATER,
		WordAtom.LOWER),
	MEMBER(
		WordAtom.VAR,
		WordAtom.FUN),
	GENERICS_START(
		WordAtom.LOWER),
	GENERICS_END(
		WordAtom.GREATER),
	TYPE_TYPE(
		WordAtom.CLASS,
		WordAtom.GENERIC,
		WordAtom.OBJECT);

	override fun includes(atom: WordAtom?): Boolean {
		return atoms.contains(atom)
	}
}