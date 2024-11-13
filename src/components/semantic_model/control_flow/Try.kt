package components.semantic_model.control_flow

import components.code_generation.llvm.models.control_flow.Try
import components.semantic_model.scopes.Scope
import components.semantic_model.types.OptionalType
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.Try as TrySyntaxTree

class Try(override val source: TrySyntaxTree, scope: Scope, val expression: Value, val isOptional: Boolean): Value(source, scope) {

	init {
		addSemanticModels(expression)
	}

	override fun determineTypes() {
		super.determineTypes()
		expression.providedType?.let { expressionType ->
			providedType = if(isOptional)
				OptionalType(source, scope, expressionType) //TODO write tests: NULL / NOTHING should not be optional
			else
				expressionType
		}
	}

	override fun toUnit() = Try(this, expression.toUnit())
}
