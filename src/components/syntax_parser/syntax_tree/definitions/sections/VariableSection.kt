package components.syntax_parser.syntax_tree.definitions.sections

import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.tokenizer.Word
import components.tokenizer.WordAtom
import source_structure.Position
import util.indent
import util.toLines

class VariableSection(val declarationType: Word, val type: TypeSyntaxTreeNode?, val value: ValueSyntaxTreeNode?,
					  val variables: List<VariableSectionSyntaxTreeNode>, end: Position):
	DeclarationSection(declarationType.start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null
	val isConstant: Boolean
		get() = declarationType.type == WordAtom.VAL

	init {
		for(variable in variables)
			variable.parent = this
	}

	override fun toSemanticModel(scope: MutableScope, semanticModels: MutableList<SemanticModel>) {
		for(variable in variables)
			variable.toSemanticModel(scope, semanticModels)
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
