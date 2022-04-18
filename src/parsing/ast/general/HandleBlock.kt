package parsing.ast.general

import linter.Linter
import linter.elements.general.HandleBlock
import linter.elements.values.VariableValueDeclaration
import linter.scopes.Scope
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type
import source_structure.Position

class HandleBlock(start: Position, private val type: Type, private val identifier: Identifier?, private val block: StatementBlock): Element(start, block.end) {

	override fun concretize(linter: Linter, scope: Scope): HandleBlock {
		val variableValueDeclaration = if(identifier == null)
			null
		else
			VariableValueDeclaration(identifier, identifier.getValue(), true)
		return HandleBlock(this, type.concretize(linter, scope), variableValueDeclaration, block.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Handle [ $type${if(identifier == null) "" else " $identifier"} ] { $block }"
	}
}