package components.semantic_analysis.semantic_model.operations

import errors.user.SignatureResolutionAmbiguityError
import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.OperatorDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import messages.Message
import components.syntax_parser.syntax_tree.operations.UnaryOperator as UnaryOperatorSyntaxTree

class UnaryOperator(override val source: UnaryOperatorSyntaxTree, val value: Value, val kind: OperatorDefinition.Kind):
	Value(source) {

	init {
		addUnits(value)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		value.type?.let { valueType ->
			try {
				val operatorDefinition = valueType.scope.resolveOperator(kind)
				if(operatorDefinition == null) {
					linter.addMessage(source, "Operator '$kind$valueType' hasn't been declared yet.",
						Message.Type.ERROR)
					return@let
				}
				type = operatorDefinition.returnType
			} catch(error: SignatureResolutionAmbiguityError) {
				linter.addMessage(source, "Call to operator '$kind$valueType' is ambiguous. " +
					"Matching signatures:" + error.getSignatureList(), Message.Type.ERROR) //TODO write test for this
			}
		}
		staticValue = calculateStaticResult()
	}

	private fun calculateStaticResult(): Value? = null //TODO implement
}
