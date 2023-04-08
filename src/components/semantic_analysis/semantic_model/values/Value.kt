package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import errors.internal.CompilerError
import logger.issues.resolution.MissingType

abstract class Value(override val source: Element, override var scope: Scope, var type: Type? = null): Unit(source, scope) {
	open var staticValue: Value? = null
	protected var positiveState: VariableTracker.VariableState? = null
	protected var negativeState: VariableTracker.VariableState? = null

	open fun isAssignableTo(targetType: Type?): Boolean {
		return type?.let { type -> targetType?.accepts(type) } ?: false
	}

	open fun setInferredType(inferredType: Type?) {
		if(type == null) {
			type = if(inferredType is OptionalType)
				inferredType.baseType
			else
				inferredType
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		setEndStates(tracker)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(type == null)
			linter.addIssue(MissingType(source))
	}

	fun setEndStates(tracker: VariableTracker) {
		val currentState = tracker.currentState.copy()
		positiveState = currentState
		negativeState = currentState
	}

	fun setEndState(tracker: VariableTracker, isPositive: Boolean) = setEndState(tracker.currentState.copy(), isPositive)

	fun setEndState(state: VariableTracker.VariableState, isPositive: Boolean) {
		if(isPositive)
			positiveState = state
		else
			negativeState = state
	}

	fun getEndState(isPositive: Boolean): VariableTracker.VariableState {
		return if(isPositive) getPositiveEndState() else getNegativeEndState()
	}

	open fun getPositiveEndState(): VariableTracker.VariableState {
		return positiveState ?: throw CompilerError(source, "Tried to access missing positive state.")
	}

	open fun getNegativeEndState(): VariableTracker.VariableState {
		return negativeState ?: throw CompilerError(source, "Tried to access missing negative state.")
	}

	open fun getComputedValue(tracker: VariableTracker): Value? = staticValue //TODO get this from dataflow
	open fun getComputedType(tracker: VariableTracker): Type? = getComputedValue(tracker)?.type

	override fun hashCode(): Int {
		return type.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		if(other !is Value)
			return false
		return type == other.type
	}

	override fun toString(): String {
		return "${javaClass.simpleName}#${hashCode()}"
	}

//	abstract override fun compile(context: BuildContext): LLVMValueRef
}
