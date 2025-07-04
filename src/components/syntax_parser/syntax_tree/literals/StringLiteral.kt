package components.syntax_parser.syntax_tree.literals

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.tokenizer.Word
import util.indent
import util.toLines
import components.semantic_model.values.StringLiteral as SemanticStringLiteralModel
import components.semantic_model.values.StringPart as SemanticStringPartModel

class StringLiteral(word: Word, val parts: List<StringPart>): ValueSyntaxTreeNode(word) {

	override fun toSemanticModel(scope: MutableScope): SemanticStringLiteralModel {
		return SemanticStringLiteralModel(this, scope, parts.map { part -> part.toSemanticModel(scope) })
	}

	override fun toString(): String {
		val stringRepresentation = StringBuilder()
		stringRepresentation.append("StringLiteral {")
		stringRepresentation.append(
			parts.map { part -> if(part is StringPart.Segment) "\"${part.value}\"" else part.toString() }.toLines().indent())
		stringRepresentation.append("\n}")
		return stringRepresentation.toString()
	}
}

sealed class StringPart {
	abstract fun toSemanticModel(scope: MutableScope): SemanticStringPartModel

	class Segment(val value: String): StringPart() {
		override fun toSemanticModel(scope: MutableScope) = SemanticStringPartModel.Segment(value)
	}

	class Template(val expression: ValueSyntaxTreeNode): StringPart() {
		override fun toSemanticModel(scope: MutableScope) = SemanticStringPartModel.Template(expression.toSemanticModel(scope))
		override fun toString() = expression.toString()
	}
}
