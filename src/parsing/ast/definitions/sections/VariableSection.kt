package parsing.ast.definitions.sections

import linter.Linter
import linter.elements.general.Unit
import linter.scopes.MutableScope
import parsing.ast.general.Element
import parsing.ast.general.TypeElement
import parsing.tokenizer.Word
import parsing.tokenizer.WordAtom
import source_structure.Position
import util.indent
import util.toLines
import java.lang.StringBuilder

class VariableSection(val declarationType: Word, val type: TypeElement?, val value: Element?,
					  val variables: List<VariableSectionElement>, end: Position):
	DeclarationSection(declarationType.start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null
	val isConstant: Boolean
		get() = declarationType.type == WordAtom.VAL

	init {
		for(variable in variables)
			variable.parent = this
	}

	override fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>) {
		for(variable in variables)
			variable.concretize(linter, scope, units)
	}

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