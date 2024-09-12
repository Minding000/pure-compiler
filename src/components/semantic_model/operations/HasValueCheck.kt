package components.semantic_model.operations

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.scopes.Scope
import components.semantic_model.types.LiteralType
import components.semantic_model.types.OptionalType
import components.semantic_model.values.BooleanLiteral
import components.semantic_model.values.NullLiteral
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import logger.issues.constant_conditions.StaticHasValueCheckResult
import components.syntax_parser.syntax_tree.operations.HasValueCheck as HasValueCheckSyntaxTree

class HasValueCheck(override val source: HasValueCheckSyntaxTree, scope: Scope, val subject: Value): Value(source, scope) {

	init {
		providedType = LiteralType(source, scope, SpecialType.BOOLEAN)
		addSemanticModels(subject, providedType)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		val subjectVariable = subject as? VariableValue
		val subjectVariableDeclaration = subjectVariable?.declaration
		if(subjectVariableDeclaration != null) {
			val commonState = tracker.currentState.copy()
			val variableType = subjectVariable.providedType as? OptionalType
			if(variableType != null) {
				tracker.add(VariableUsage.Kind.HINT, subjectVariableDeclaration, this, variableType.baseType)
				positiveState = tracker.currentState.copy()
			}
			tracker.setVariableStates(commonState)
			val nullLiteral = NullLiteral(this)
			tracker.add(VariableUsage.Kind.HINT, subjectVariableDeclaration, this, nullLiteral.providedType, nullLiteral)
			negativeState = tracker.currentState.copy()
			tracker.setVariableStates(commonState)
		}
		computeStaticValue()
	}

	private fun computeStaticValue() {
		val subjectType = subject.getComputedType()
		staticValue = if(subjectType == null) {
			null
		} else if(SpecialType.NULL.matches(subjectType)) {
			context.addIssue(StaticHasValueCheckResult(source, "no"))
			BooleanLiteral(this, false)
		} else if(subjectType !is OptionalType) {
			context.addIssue(StaticHasValueCheckResult(source, "yes"))
			BooleanLiteral(this, true)
		} else null
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		if(SpecialType.NULL.matches(subject.effectiveType))
			return constructor.buildBoolean(false)
		if(subject.effectiveType !is OptionalType)
			return constructor.buildBoolean(true)
		return constructor.buildIsNotNull(subject.getLlvmValue(constructor), "_hasValueCheck_result")
	}
}
