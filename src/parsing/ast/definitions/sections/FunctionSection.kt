package parsing.ast.definitions.sections

import parsing.ast.general.Element
import parsing.ast.general.MetaElement
import parsing.tokenizer.Word
import source_structure.Position
import util.indent
import util.toLines
import java.lang.StringBuilder

class FunctionSection(private val declarationType: Word, private val variables: List<Element>,
					  end: Position): DeclarationSection(declarationType.start, end) {

	override fun toString(): String {
		val string = StringBuilder()
		string
			.append("FunctionSection [ ")
			.append(declarationType.getValue())
			.append(" ] {")
			.append(variables.toLines().indent())
			.append("\n}")
		return string.toString()
	}
}