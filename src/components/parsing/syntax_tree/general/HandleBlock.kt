package components.parsing.syntax_tree.general

import linting.Linter
import linting.semantic_model.general.HandleBlock as SemanticHandleBlockModel
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.values.VariableValueDeclaration
import components.parsing.syntax_tree.literals.Identifier
import source_structure.Position

class HandleBlock(start: Position, private val type: TypeElement, private val identifier: Identifier?,
				  private val block: StatementBlock): Element(start, block.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticHandleBlockModel {
		val blockScope = BlockScope(scope)
		val eventType = type.concretize(linter, scope)
		val variableValueDeclaration = if(identifier != null) {
			val declaration = VariableValueDeclaration(identifier, identifier.getValue(), eventType)
			blockScope.declareValue(linter, declaration)
			declaration
		} else null
		return SemanticHandleBlockModel(this, blockScope, eventType, variableValueDeclaration,
			block.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Handle [ $type${if(identifier == null) "" else " $identifier"} ] { $block }"
	}
}
