package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position
import util.indent
import util.toLines
import util.toSemanticValueModels
import components.semantic_model.values.Instance as SemanticInstanceModel

class Instance(val identifier: Identifier, val parameters: List<ValueSyntaxTreeNode>, end: Position): SyntaxTreeNode(identifier.start, end) {

	override fun toSemanticModel(scope: MutableScope): SemanticInstanceModel {
		return SemanticInstanceModel(this, scope, identifier.getValue(), parameters.toSemanticValueModels(scope))
	}

	override fun toString(): String {
		return "Instance [ $identifier ] {${parameters.toLines().indent()}\n}"
	}
}
