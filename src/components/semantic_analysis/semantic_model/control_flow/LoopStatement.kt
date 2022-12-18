package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.syntax_parser.syntax_tree.control_flow.LoopStatement as LoopStatementSyntaxTree

class LoopStatement(override val source: LoopStatementSyntaxTree, val scope: BlockScope, val generator: Unit?,
					val body: ErrorHandlingContext): Unit(source) {
	override var isInterruptingExecution = false
	var mightGetBrokenOutOf = false

	init {
		scope.unit = this
		addUnits(generator, body)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		var hasFiniteGenerator = true
		if(generator == null) {
			hasFiniteGenerator = false
		} else if(generator is WhileGenerator) {
			val condition = generator.condition.staticValue
			if(condition is BooleanLiteral && condition.value)
				hasFiniteGenerator = false
		}
		if(!(hasFiniteGenerator || mightGetBrokenOutOf))
			isInterruptingExecution = true
	}
}
