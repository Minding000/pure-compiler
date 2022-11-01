package components.linting.semantic_model.operations

import errors.user.SignatureResolutionAmbiguityError
import components.linting.Linter
import components.linting.semantic_model.scopes.Scope
import components.linting.semantic_model.values.Value
import messages.Message
import components.syntax_parser.syntax_tree.operations.UnaryOperator as UnaryOperatorSyntaxTree

class UnaryOperator(override val source: UnaryOperatorSyntaxTree, val value: Value, val operatorName: String):
	Value(source) {

	init {
		units.add(value)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		value.type?.let { valueType ->
			try {
				val operatorDefinition = valueType.scope.resolveOperator(operatorName)
				if(operatorDefinition == null) {
					linter.addMessage(source, "Operator '$operatorName$valueType' hasn't been declared yet.",
						Message.Type.ERROR)
					return@let
				}
				type = operatorDefinition.returnType
			} catch(error: SignatureResolutionAmbiguityError) {
				linter.addMessage(source, "Call to operator '$operatorName$valueType' is ambiguous. " +
					"Matching signatures:" + error.signatures.joinToString("\n - ", "\n - "),
					Message.Type.ERROR) //TODO write test for this
			}
		}
		staticValue = calculateStaticResult()
	}

	private fun calculateStaticResult(): Value? = null //TODO implement
}
