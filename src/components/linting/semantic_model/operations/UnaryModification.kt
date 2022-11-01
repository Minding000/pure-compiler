package components.linting.semantic_model.operations

import errors.user.SignatureResolutionAmbiguityError
import components.linting.Linter
import components.linting.semantic_model.general.Unit
import components.linting.semantic_model.scopes.Scope
import components.linting.semantic_model.values.Value
import messages.Message
import components.syntax_parser.syntax_tree.operations.UnaryModification as UnaryModificationSyntaxTree

class UnaryModification(override val source: UnaryModificationSyntaxTree, val target: Value, val operatorName: String):
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
