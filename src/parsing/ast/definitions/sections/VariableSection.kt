package parsing.ast.definitions.sections

import parsing.ast.general.Element
import parsing.ast.literals.Type
import parsing.tokenizer.Word
import parsing.tokenizer.WordAtom
import source_structure.Position
import util.indent
import util.toLines
import java.lang.StringBuilder

class VariableSection(val declarationType: Word, val type: Type?, val value: Element?,
					  val variables: List<Element>, end: Position): DeclarationSection(declarationType.start, end) {

	val isConstant: Boolean
		get() = declarationType.type == WordAtom.VAL

	override fun toString(): String {
		val string = StringBuilder()
		string
			.append("VariableSection [ ")
			.append(declarationType.getValue())
		if(type != null)
			string.append(": ").append(type)
		if(value != null)
			string.append(" = ").append(value)
		string
			.append(" ] {")
			.append(variables.toLines().indent())
			.append("\n}")
		return string.toString()
	}
}