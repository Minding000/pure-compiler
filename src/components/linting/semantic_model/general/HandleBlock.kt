package components.linting.semantic_model.general

import components.linting.Linter
import components.linting.semantic_model.types.Type
import components.linting.semantic_model.scopes.BlockScope
import components.linting.semantic_model.scopes.Scope
import components.linting.semantic_model.values.VariableValueDeclaration
import components.parsing.syntax_tree.general.HandleBlock as HandleBlockSyntaxTree

class HandleBlock(override val source: HandleBlockSyntaxTree, val scope: BlockScope, val eventType: Type,
				  val eventVariable: VariableValueDeclaration?, val block: StatementBlock): Unit(source) {

	init {
		units.add(eventType)
		if(eventVariable != null)
			units.add(eventVariable)
		units.add(block)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}
}
