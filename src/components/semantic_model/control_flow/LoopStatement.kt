package components.semantic_model.control_flow

import components.code_generation.llvm.models.control_flow.LoopStatement
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.BlockScope
import components.semantic_model.values.BooleanLiteral
import components.syntax_parser.syntax_tree.control_flow.LoopStatement as LoopStatementSyntaxTree

class LoopStatement(override val source: LoopStatementSyntaxTree, override val scope: BlockScope, val generator: SemanticModel?,
					val body: ErrorHandlingContext): SemanticModel(source, scope) {
	override var isInterruptingExecutionBasedOnStructure = false
	override var isInterruptingExecutionBasedOnStaticEvaluation = false
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
	val mutatedVariables = HashSet<ValueDeclaration>()
	lateinit var unit: LoopStatement

	init {
		scope.semanticModel = this
		addSemanticModels(generator, body)
	}

	override fun determineTypes() {
		context.surroundingLoops.add(this)
		super.determineTypes()
		context.surroundingLoops.remove(this)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		for(declaration in mutatedVariables)
			tracker.add(VariableUsage.Kind.HINT, declaration, this, declaration.providedType, null)
		val isPostCondition = generator is WhileGenerator && generator.isPostCondition
		val (loopReferencePoint, loopEndState) = if(generator is WhileGenerator) {
			val referencePoint = tracker.currentState.createReferencePoint()
			if(isPostCondition)
				body.analyseDataFlow(tracker)
			generator.analyseDataFlow(tracker)
			if(generator.isExitCondition) {
				tracker.setVariableStates(generator.condition.getNegativeEndState())
				Pair(referencePoint, generator.condition.getPositiveEndState())
			} else {
				tracker.setVariableStates(generator.condition.getPositiveEndState())
				Pair(referencePoint, generator.condition.getNegativeEndState())
			}
		} else {
			generator?.analyseDataFlow(tracker)
			Pair(tracker.currentState.createReferencePoint(), tracker.currentState.copy())
		}
		if(!isPostCondition)
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
		if(!(hasFiniteGenerator || mightGetBrokenOutOf)) {
			isInterruptingExecutionBasedOnStructure = true
			isInterruptingExecutionBasedOnStaticEvaluation = true
		}
	}

	override fun validate() {
		super.validate()
		scope.validate()
	}

	override fun toUnit(): LoopStatement {
		val unit = LoopStatement(this, generator?.toUnit(), body.toUnit())
		this.unit = unit
		return unit
	}
}
