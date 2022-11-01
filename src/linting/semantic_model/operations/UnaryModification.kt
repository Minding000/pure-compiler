package linting.semantic_model.operations

import errors.user.SignatureResolutionAmbiguityError
import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import messages.Message
import components.parsing.syntax_tree.operations.UnaryModification

class UnaryModification(override val source: UnaryModification, val target: Value, val operatorName: String):
	Unit(source) {

	init {
		units.add(target)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		target.type?.let { valueType ->
			try {
				val operatorDefinition = valueType.scope.resolveOperator(operatorName)
				if(operatorDefinition == null) {
					linter.addMessage(source, "Operator '$valueType$operatorName' hasn't been declared yet.",
						Message.Type.ERROR) //TODO write test for this
				}
			} catch(error: SignatureResolutionAmbiguityError) {
				linter.addMessage(source, "Call to operator '$valueType$operatorName' is ambiguous. " +
					"Matching signatures:" + error.signatures.joinToString("\n - ", "\n - "),
					Message.Type.ERROR) //TODO write test for this
			}
		}
	}
}
