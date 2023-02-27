package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position
import components.semantic_analysis.semantic_model.general.HandleBlock as SemanticHandleBlockModel

class HandleBlock(start: Position, private val type: TypeElement, private val identifier: Identifier?, private val block: StatementBlock):
	Element(start, block.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticHandleBlockModel {
		val statementBlock = block.concretize(linter, scope)
		val eventType = type.concretize(linter, scope)
		val variableValueDeclaration = if(identifier != null) {
			val declaration = LocalVariableDeclaration(identifier, scope, eventType)
			statementBlock.scope.declareValue(linter, declaration)
			declaration
		} else null
		return SemanticHandleBlockModel(this, scope, eventType, variableValueDeclaration, statementBlock)
	}

	override fun toString(): String {
		return "Handle [ $type${if(identifier == null) "" else " $identifier"} ] { $block }"
	}
}
