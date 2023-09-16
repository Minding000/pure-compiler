package components.syntax_parser.syntax_tree.literals

import components.semantic_model.scopes.MutableScope
import components.semantic_model.values.VariableValue
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.tokenizer.Word

open class Identifier(word: Word): ValueSyntaxTreeNode(word) {

	override fun toSemanticModel(scope: MutableScope): VariableValue {
		return VariableValue(this, scope)
	}

	override fun toString(): String {
		return "Identifier { ${getValue()} }"
	}
}
