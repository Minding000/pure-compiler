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

	override fun analyseDataFlow(tracker: VariableTracker) {
		val (loopReferencePoint, loopEndState) = if(generator is WhileGenerator) {
			val referencePoint = tracker.currentState.createReferencePoint()
			generator.analyseDataFlow(tracker)
			tracker.setVariableStates(generator.condition.getPositiveEndState())
			Pair(referencePoint, generator.condition.getNegativeEndState())
		} else {
			generator?.analyseDataFlow(tracker)
			Pair(tracker.currentState.createReferencePoint(), tracker.currentState.copy())
		}
		body.analyseDataFlow(tracker)
		tracker.linkBackTo(loopReferencePoint)
		for(variableState in tracker.nextStatementStates)
			tracker.link(variableState, loopReferencePoint)
		tracker.nextStatementStates.clear()
		if(hasFiniteGenerator) {
			if(generator is WhileGenerator)
				tracker.setVariableStates(loopEndState)
			else
				tracker.addVariableStates(loopEndState)
		} else {
			tracker.currentState.lastVariableUsages.clear()
		}
		tracker.addVariableStates(tracker.breakStatementStates)
		tracker.breakStatementStates.clear()
		tracker.currentState.removeReferencePoint(loopReferencePoint)
	}

	override fun validate() {
		super.validate()
		if(!(hasFiniteGenerator || mightGetBrokenOutOf))
			isInterruptingExecution = true
	}
}
