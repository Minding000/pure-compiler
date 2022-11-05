package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.HandleBlock as SemanticHandleBlockModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.values.VariableValueDeclaration
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position

class HandleBlock(start: Position, private val type: TypeElement, private val identifier: Identifier?,
				  private val block: StatementBlock): Element(start, block.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticHandleBlockModel {
		val statementBlock = block.concretize(linter, scope)
		val eventType = type.concretize(linter, scope)
		val variableValueDeclaration = if(identifier != null) {
			val declaration = VariableValueDeclaration(identifier, identifier.getValue(), eventType)
			statementBlock.scope.declareValue(linter, declaration)
			declaration
		} else null
		return SemanticHandleBlockModel(this, eventType, variableValueDeclaration, statementBlock)
	}

	override fun toString(): String {
		return "Handle [ $type${if(identifier == null) "" else " $identifier"} ] { $block }"
	}
}
