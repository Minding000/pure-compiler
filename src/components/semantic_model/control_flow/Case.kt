package components.semantic_model.control_flow

import components.code_generation.llvm.models.control_flow.Case
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.Case as CaseSyntaxTree

class Case(override val source: CaseSyntaxTree, scope: Scope, val condition: Value, val result: ErrorHandlingContext):
	SemanticModel(source, scope) {

	init {
		addSemanticModels(condition, result)
	}

	override fun toUnit() = Case(this, condition.toUnit(), result.toUnit())
}
