package parsing.syntax_tree.definitions.sections

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.TypeElement
import parsing.syntax_tree.general.ValueElement
import components.tokenizer.Word
import components.tokenizer.WordAtom
import source_structure.Position
import util.indent
import util.toLines
import java.lang.StringBuilder

class VariableSection(val declarationType: Word, val type: TypeElement?, val value: ValueElement?,
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
