package components.syntax_parser.syntax_tree.control_flow

import components.semantic_model.declarations.LocalVariableDeclaration
import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position
import util.indent
import util.toLines
import components.semantic_model.control_flow.OverGenerator as SemanticOverGeneratorModel

class OverGenerator(start: Position, private val collection: ValueSyntaxTreeNode, private val iteratorVariableDeclaration: Identifier?,
					private val variableDeclarations: List<Identifier>):
	SyntaxTreeNode(start, variableDeclarations.lastOrNull()?.end ?: collection.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticOverGeneratorModel {
		return SemanticOverGeneratorModel(this, scope, collection.toSemanticModel(scope),
			iteratorVariableDeclaration?.let { variableDeclaration -> LocalVariableDeclaration(variableDeclaration, scope) },
			variableDeclarations.map { variableDeclaration -> LocalVariableDeclaration(variableDeclaration, scope) })
	}

	override fun toString(): String {
		var stringRepresentation = "OverGenerator [ $collection"
		if(iteratorVariableDeclaration != null)
			stringRepresentation += " using $iteratorVariableDeclaration"
		stringRepresentation += " ] {"
		stringRepresentation += variableDeclarations.toLines().indent()
		stringRepresentation += "\n}"
		return stringRepresentation
	}
}
