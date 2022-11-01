package components.parsing.syntax_tree.definitions.sections

import components.linting.Linter
import components.linting.semantic_model.general.Unit
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.definitions.OperatorDefinition
import components.tokenizer.Word
import source_structure.Position
import util.indent
import util.toLines
import java.lang.StringBuilder

class OperatorSection(declarationType: Word, private val operators: List<OperatorDefinition>,
					  end: Position): DeclarationSection(declarationType.start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	init {
		for(operator in operators)
			operator.parent = this
	}

	override fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>) {
		for(function in operators)
			function.concretize(linter, scope, units)
	}

	override fun toString(): String {
		val string = StringBuilder()
		string
			.append("OperatorSection {")
			.append(operators.toLines().indent())
			.append("\n}")
		return string.toString()
	}
}
