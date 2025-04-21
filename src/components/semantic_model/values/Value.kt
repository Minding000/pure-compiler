package components.semantic_model.values

import components.semantic_model.context.VariableTracker
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.OptionalType
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import logger.issues.resolution.MissingType
import components.code_generation.llvm.models.values.Value as ValueUnit

open class Value(override val source: SyntaxTreeNode, override var scope: Scope, var providedType: Type? = null):
	SemanticModel(source, scope) {
	val effectiveType: Type? get() = providedType?.effectiveType
	protected open var staticValue: Value? = null
	protected var positiveState: VariableTracker.VariableState? = null
	protected var negativeState: VariableTracker.VariableState? = null
	open val hasGenericType = false

	open fun isAssignableTo(targetType: Type?): Boolean {
		return providedType?.let { type -> targetType?.accepts(type) } ?: false
	}

	fun setUnextendedType(type: Type?) {
		val surroundingComputedProperty = scope.getSurroundingComputedProperty()
		var whereClauseCondition = surroundingComputedProperty?.whereClauseConditions?.find { condition -> condition.matches(type) }
		if(whereClauseCondition != null) {
			providedType = ObjectType(whereClauseCondition)
			addSemanticModels(providedType)
			return
		}
		val surroundingFunction = scope.getSurroundingFunction()
		whereClauseCondition = surroundingFunction?.signature?.whereClauseConditions?.find { condition -> condition.matches(type) }
		if(whereClauseCondition != null) {
			providedType = ObjectType(whereClauseCondition)
			addSemanticModels(providedType)
			return
		}
		providedType = type
	}

	open fun setInferredType(inferredType: Type?) {
		if(providedType == null) {
			providedType = if(inferredType is OptionalType)
				inferredType.baseType
			else
				inferredType
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		setEndStates(tracker)
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

	fun getComputedValue(): Value? = staticValue
	open fun getComputedType(): Type? = getComputedValue()?.providedType ?: providedType

	/** The provided type adjusted by computed nullability */
	fun getDisplayType(): Type? {
		val providedType = providedType
		val computedType = getComputedType()
		return if(providedType is OptionalType && computedType != null && computedType !is OptionalType) providedType.baseType else providedType
	}

	override fun validate() {
		super.validate()
		if(providedType == null)
			context.addIssue(MissingType(source))
	}

	override fun toUnit(): ValueUnit {
		if(this.javaClass.simpleName == "Value") {
			return ValueUnit(this)
		}
		TODO("${source.getStartString()}: '${javaClass.simpleName}.toUnit' is not implemented yet.")
	}

	override fun hashCode(): Int {
		return providedType.hashCode()
	}

	// Note: Just here to suppress a warning
	override fun equals(other: Any?): Boolean {
		if(other !is Value)
			return false
		return super.equals(other)
	}

	override fun toString(): String {
		return "${javaClass.simpleName}#${hashCode()}"
	}
}
