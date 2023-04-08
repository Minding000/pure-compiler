package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.syntax_parser.syntax_tree.control_flow.LoopStatement as LoopStatementSyntaxTree

class LoopStatement(override val source: LoopStatementSyntaxTree, override val scope: BlockScope, val generator: Unit?,
					val body: ErrorHandlingContext): Unit(source, scope) {
	override var isInterruptingExecution = false
	var mightGetBrokenOutOf = false
	private val hasFiniteGenerator: Boolean
		get() {
			if(generator == null) {
				return false
			} else if(generator is WhileGenerator) {
				val condition = generator.condition.staticValue
				if(condition is BooleanLiteral && condition.value)
					return false
			}
			return true
		}

	init {
		scope.unit = this
		addUnits(generator, body)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		val initialState = tracker.currentState.copy()
		generator?.analyseDataFlow(tracker)
		tracker.currentState.firstVariableUsages.clear()
		body.analyseDataFlow(tracker)
		tracker.linkBackToStart()
		for(variableState in tracker.nextStatementStates)
			tracker.linkBackToStartFrom(variableState)
		tracker.nextStatementStates.clear()
		if(hasFiniteGenerator)
			tracker.addVariableStates(initialState)
		else
			tracker.currentState.lastVariableUsages.clear()
		tracker.addVariableStates(*tracker.breakStatementStates.toTypedArray())
		tracker.breakStatementStates.clear()
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(!(hasFiniteGenerator || mightGetBrokenOutOf))
			isInterruptingExecution = true
	}
}
