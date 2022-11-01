package components.syntax_parser.syntax_tree.definitions.sections

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.FunctionDefinition
import components.tokenizer.Word
import source_structure.Position
import util.indent
import util.toLines
import java.lang.StringBuilder

class FunctionSection(private val declarationType: Word, private val functions: List<FunctionDefinition>,
					  end: Position): DeclarationSection(declarationType.start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	init {
		for(function in functions)
			function.parent = this
	}

	override fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>) {
		for(function in functions)
			function.concretize(linter, scope, units)
	}

	override fun toString(): String {
		val string = StringBuilder()
		string
			.append("FunctionSection [ ")
			.append(declarationType.getValue())
			.append(" ] {")
			.append(functions.toLines().indent())
			.append("\n}")
		return string.toString()
	}
}
