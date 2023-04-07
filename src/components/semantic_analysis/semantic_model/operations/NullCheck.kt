package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
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
		type = LiteralType(source, scope, Linter.SpecialType.BOOLEAN)
		addUnits(value, type)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		value.staticValue?.type?.let { staticType ->
			staticValue = if(Linter.SpecialType.NULL.matches(staticType)) {
				linter.addIssue(StaticNullCheckValue(source, "no"))
				BooleanLiteral(source, scope, false, linter)
			} else if(staticType !is OptionalType) {
				linter.addIssue(StaticNullCheckValue(source, "yes"))
				BooleanLiteral(source, scope, true, linter)
			} else null
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		super.analyseDataFlow(linter, tracker)
		val variableValue = value as? VariableValue
		val declaration = variableValue?.definition
		if(declaration != null) {
			val commonState = tracker.currentState.copy()
			val nullLiteral = NullLiteral(source, scope, linter)
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
	}
}
