package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.syntax_parser.syntax_tree.control_flow.LoopStatement as LoopStatementSyntaxTree

class LoopStatement(override val source: LoopStatementSyntaxTree, override val scope: BlockScope, val generator: SemanticModel?,
					val body: ErrorHandlingContext): SemanticModel(source, scope) {
	override var isInterruptingExecution = false
	var mightGetBrokenOutOf = false
	private val hasFiniteGenerator: Boolean
		get() {
			if(generator == null) {
				return false
			} else if(generator is WhileGenerator) {
				val condition = generator.condition.getComputedValue()
				if(condition is BooleanLiteral && condition.value)
					return false
			}
			return true
		}

	init {
		scope.semanticModel = this
		addSemanticModels(generator, body)
	}

	override fun analyseDataFlow(tracker: VariableTracker) { //TODO test nested loops (with break etc.)
		val postConditionState = if(generator is WhileGenerator) {
			tracker.currentState.firstVariableUsages.clear() //TODO first usages should be stored in reference points that are added and removed
			generator.analyseDataFlow(tracker)
			tracker.setVariableStates(generator.condition.getPositiveEndState())
			generator.condition.getNegativeEndState()
		} else {
			generator?.analyseDataFlow(tracker)
			tracker.currentState.firstVariableUsages.clear()
			tracker.currentState.copy()
		}
		body.analyseDataFlow(tracker)
		tracker.linkBackToStart()
		for(variableState in tracker.nextStatementStates)
			tracker.linkBackToStartFrom(variableState)
		tracker.nextStatementStates.clear()
		if(hasFiniteGenerator) {
			if(generator is WhileGenerator)
				tracker.setVariableStates(postConditionState)
			else
				tracker.addVariableStates(postConditionState)
		} else {
			tracker.currentState.lastVariableUsages.clear()
		}
		tracker.addVariableStates(*tracker.breakStatementStates.toTypedArray()) //TODO refactor: accept collection as parameter
		tracker.breakStatementStates.clear()
	}

	override fun validate() {
		super.validate()
		if(!(hasFiniteGenerator || mightGetBrokenOutOf))
			isInterruptingExecution = true
	}
}
