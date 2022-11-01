package linting.semantic_model.operations

import errors.user.SignatureResolutionAmbiguityError
import linting.Linter
import linting.semantic_model.values.Value
import messages.Message
import linting.semantic_model.scopes.Scope
import components.parsing.syntax_tree.operations.BinaryOperator as BinaryOperatorSyntaxTree

class BinaryOperator(override val source: BinaryOperatorSyntaxTree, val left: Value, val right: Value,
					 val operatorName: String): Value(source) {

	init {
		units.add(left)
		units.add(right)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		left.type?.let { leftType ->
			try {
				val operatorDefinition = leftType.scope.resolveOperator(operatorName, right)
				if(operatorDefinition == null) {
					linter.addMessage(source,
						"Operator '$leftType $operatorName ${right.type}' hasn't been declared yet.",
						Message.Type.ERROR)
					return@let
				}
				type = operatorDefinition.returnType
			} catch(error: SignatureResolutionAmbiguityError) {
				linter.addMessage(source,
					"Call to operator '$leftType $operatorName ${right.type}' is ambiguous. " +
					"Matching signatures:" + error.signatures.joinToString("\n - ", "\n - "),
					Message.Type.ERROR) //TODO write test for this
			}
		}
		staticValue = calculateStaticResult()
	}

	private fun calculateStaticResult(): Value? = null //TODO implement
}
