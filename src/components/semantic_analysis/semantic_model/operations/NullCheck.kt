package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.context.VariableUsage
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.NullLiteral
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.issues.constant_conditions.StaticNullCheckValue
import components.syntax_parser.syntax_tree.operations.NullCheck as NullCheckSyntaxTree

class NullCheck(override val source: NullCheckSyntaxTree, scope: Scope, val value: Value): Value(source, scope) {

	init {
		type = LiteralType(source, scope, SpecialType.BOOLEAN)
		addSemanticModels(value, type)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		val variableValue = value as? VariableValue
		val declaration = variableValue?.definition
		if(declaration != null) {
			val commonState = tracker.currentState.copy()
			val nullLiteral = NullLiteral(this)
			tracker.add(VariableUsage.Kind.HINT, declaration, this, nullLiteral.type, nullLiteral)
			positiveState = tracker.currentState.copy()
			tracker.setVariableStates(commonState)
			val variableType = variableValue.type as? OptionalType
			val baseType = variableType?.baseType
			if(baseType != null) {
				tracker.add(VariableUsage.Kind.HINT, declaration, this, baseType)
				negativeState = tracker.currentState.copy()
			}
			tracker.setVariableStates(commonState)
		}
		val valueType = value.getComputedType()
		staticValue = if(valueType == null) {
			null
		} else if(SpecialType.NULL.matches(valueType)) {
			context.addIssue(StaticNullCheckValue(source, "no"))
			BooleanLiteral(this, false)
		} else if(valueType !is OptionalType) {
			context.addIssue(StaticNullCheckValue(source, "yes"))
			BooleanLiteral(this, true)
		} else null
	}
}
