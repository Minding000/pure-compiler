package components.syntax_parser.syntax_tree.operations

import components.semantic_model.scopes.MutableScope
import components.semantic_model.values.LocalVariableDeclaration
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import errors.internal.CompilerError
import util.indent
import components.semantic_model.operations.Cast as SemanticCastModel

class Cast(val value: ValueSyntaxTreeNode, val operator: String, val identifier: Identifier?, val type: TypeSyntaxTreeNode):
	ValueSyntaxTreeNode(value.start, type.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticCastModel {
		val operator = SemanticCastModel.Operator.values().find { castType ->
			castType.stringRepresentation == operator } ?: throw CompilerError(this, "Unknown cast operator '$operator'.")
		val variableDeclaration = if(identifier == null)
			null
		else
			LocalVariableDeclaration(identifier, scope)
		return SemanticCastModel(this, scope, value.toSemanticModel(scope), variableDeclaration,
			type.toSemanticModel(scope), operator)
	}

	override fun toString(): String {
		return "Cast {${"\n$value $operator ${if(identifier == null) "" else "$identifier: "}$type".indent()}\n}"
	}
}
