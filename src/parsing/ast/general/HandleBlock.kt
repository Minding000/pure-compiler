package parsing.ast.general

import linter.Linter
import linter.elements.general.HandleBlock
import linter.scopes.Scope
import parsing.ast.literals.Identifier
import source_structure.Position

class HandleBlock(start: Position, private val type: TypeElement, private val identifier: Identifier?, private val block: StatementBlock): Element(start, block.end) {

	override fun concretize(linter: Linter, scope: Scope): HandleBlock {
		return HandleBlock(this, type.concretize(linter, scope), identifier, block.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Handle [ $type${if(identifier == null) "" else " $identifier"} ] { $block }"
	}
}