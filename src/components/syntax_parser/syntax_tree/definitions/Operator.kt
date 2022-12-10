package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.definitions.OperatorDefinition
import components.syntax_parser.syntax_tree.general.MetaElement
import components.tokenizer.Word
import errors.internal.CompilerError
import source_structure.Position

open class Operator(start: Position, end: Position): MetaElement(start, end) {

	constructor(word: Word): this(word.start, word.end)

	fun getKind(): OperatorDefinition.Kind {
		val name = getValue()
		for(type in OperatorDefinition.Kind.values()) {
			if(type.stringRepresentation == name)
				return type
		}
		throw CompilerError("Unknown operator kind '$name' encountered.")
	}

	override fun toString(): String {
		return "Operator { ${getValue()} }"
	}
}
