package components.syntax_parser.syntax_tree.definitions.sections

import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.OperatorDefinition
import components.tokenizer.Word
import source_structure.Position
import util.indent
import util.toLines

class OperatorSection(declarationType: Word, private val operators: List<OperatorDefinition>,
					  end: Position): DeclarationSection(declarationType.start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	init {
		for(operator in operators)
			operator.parent = this
	}

	override fun toSemanticModel(scope: MutableScope, semanticModels: MutableList<SemanticModel>) {
		for(operator in operators)
			operator.toSemanticModel(scope, semanticModels)
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
