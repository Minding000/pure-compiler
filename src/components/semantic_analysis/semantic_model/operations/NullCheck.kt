package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.semantic_analysis.semantic_model.types.OptionalType
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.Value
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
}
