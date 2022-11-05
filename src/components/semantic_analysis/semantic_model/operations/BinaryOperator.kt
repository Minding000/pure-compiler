package components.semantic_analysis.semantic_model.operations

import errors.user.SignatureResolutionAmbiguityError
import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.values.Value
import messages.Message
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.operations.BinaryOperator as BinaryOperatorSyntaxTree

class BinaryOperator(override val source: BinaryOperatorSyntaxTree, val left: Value, val right: Value,
					 val operatorName: String): Value(source) {

	init {
		addUnits(left, right)
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
