package components.semantic_model.values

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.OptionalType
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import logger.issues.resolution.MissingType

open class Value(override val source: SyntaxTreeNode, override var scope: Scope, var type: Type? = null): SemanticModel(source, scope) {
	protected open var staticValue: Value? = null
	protected var positiveState: VariableTracker.VariableState? = null
	protected var negativeState: VariableTracker.VariableState? = null
	private var llvmValue: LlvmValue? = null

	open fun isAssignableTo(targetType: Type?): Boolean {
		return type?.let { type -> targetType?.accepts(type) } ?: false
	}

	fun setUnextendedType(type: Type?) {
		val surroundingComputedProperty = scope.getSurroundingComputedProperty()
		if(surroundingComputedProperty?.whereClause?.matches(type) == true) {
			this.type = ObjectType(surroundingComputedProperty.whereClause)
			addSemanticModels(this.type)
			return
		}
		val surroundingFunction = scope.getSurroundingFunction()
		if(surroundingFunction?.signature?.whereClause?.matches(type) == true) {
			this.type = ObjectType(surroundingFunction.signature.whereClause)
			addSemanticModels(this.type)
			return
		}
		this.type = type
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
	open fun getComputedType(): Type? = getComputedValue()?.type ?: type

	override fun validate() {
		super.validate()
		if(type == null)
			context.addIssue(MissingType(source))
	}

	open fun getLlvmLocation(constructor: LlvmConstructor): LlvmValue? {
		throw CompilerError(source, "Tried to access '${javaClass.simpleName}' LLVM location.")
	}

	fun getLlvmValue(constructor: LlvmConstructor): LlvmValue {
		var llvmValue = llvmValue
		if(llvmValue == null) {
			llvmValue = createLlvmValue(constructor)
			this.llvmValue = llvmValue
		}
		return llvmValue
	}

	protected open fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		TODO("${source.getStartString()}: '${javaClass.simpleName}.createLlvmValue' is not implemented yet.")
	}

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
}
