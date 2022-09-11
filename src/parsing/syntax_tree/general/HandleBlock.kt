package parsing.syntax_tree.general

import linting.Linter
import linting.semantic_model.general.HandleBlock
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.literals.Identifier
import source_structure.Position

class HandleBlock(start: Position, private val type: TypeElement, private val identifier: Identifier?, private val block: StatementBlock): Element(start, block.end) {

	override fun concretize(linter: Linter, scope: MutableScope): HandleBlock {
		return HandleBlock(this, type.concretize(linter, scope), identifier, block.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Handle [ $type${if(identifier == null) "" else " $identifier"} ] { $block }"
	}
}