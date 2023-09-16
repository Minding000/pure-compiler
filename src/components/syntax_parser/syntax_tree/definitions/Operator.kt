package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.values.Operator
import components.syntax_parser.syntax_tree.general.MetaSyntaxTreeNode
import components.tokenizer.Word
import errors.internal.CompilerError
import source_structure.Position

open class Operator(start: Position, end: Position): MetaSyntaxTreeNode(start, end) {

	constructor(word: Word): this(word.start, word.end)

	fun getKind(): Operator.Kind {
		val name = getValue()
		for(type in Operator.Kind.values()) {
			if(type.stringRepresentation == name)
				return type
		}
		throw CompilerError(this, "Unknown operator kind '$name' encountered.")
	}

	override fun toString(): String {
		return "Operator { ${getValue()} }"
	}
}
