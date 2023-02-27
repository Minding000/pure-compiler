package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Operator
import components.semantic_analysis.semantic_model.values.Value
import errors.user.SignatureResolutionAmbiguityError
import messages.Message
import components.syntax_parser.syntax_tree.operations.UnaryModification as UnaryModificationSyntaxTree

class UnaryModification(override val source: UnaryModificationSyntaxTree, scope: Scope, val target: Value, val kind: Operator.Kind):
	Unit(source, scope) {

	init {
		addUnits(target)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		target.type?.let { valueType ->
			try {
				val operatorDefinition = valueType.interfaceScope.resolveOperator(kind)
				if(operatorDefinition == null) {
					linter.addMessage(source, "Operator '$valueType$kind' hasn't been declared yet.", Message.Type.ERROR)
				}
			} catch(error: SignatureResolutionAmbiguityError) {
				//TODO write test for this
				error.log(linter, source, "operator", "$valueType$kind")
			}
		}
	}
}
