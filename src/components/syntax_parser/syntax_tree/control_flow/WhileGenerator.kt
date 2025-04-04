package components.syntax_parser.syntax_tree.control_flow

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.tokenizer.Word
import components.tokenizer.WordAtom
import util.indent
import components.semantic_model.control_flow.WhileGenerator as SemanticWhileGeneratorModel

class WhileGenerator(conditionalGeneratorWord: Word, private val condition: ValueSyntaxTreeNode, private val isPostCondition: Boolean):
	SyntaxTreeNode(conditionalGeneratorWord.start, condition.end) {
	private val isExitCondition = conditionalGeneratorWord.type == WordAtom.UNTIL

	override fun toSemanticModel(scope: MutableScope): SemanticWhileGeneratorModel {
		return SemanticWhileGeneratorModel(this, scope, condition.toSemanticModel(scope), isPostCondition, isExitCondition)
	}

	override fun toString(): String {
		return "WhileGenerator [${if(isExitCondition) "exit " else ""}${if(isPostCondition) "post" else "pre"}] {${"\n$condition".indent()}\n}"
	}
}
