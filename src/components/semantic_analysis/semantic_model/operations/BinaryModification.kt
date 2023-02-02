package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Operator
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import errors.user.SignatureResolutionAmbiguityError
import messages.Message
import components.syntax_parser.syntax_tree.operations.BinaryModification as BinaryModificationSyntaxTree

class BinaryModification(override val source: BinaryModificationSyntaxTree, val target: Value, val modifier: Value,
						 val kind: Operator.Kind): Unit(source) {

	init {
		addUnits(target, modifier)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		target.type?.let { valueType ->
			try {
				val operatorDefinition = valueType.scope.resolveOperator(kind, listOf(modifier))
				if(operatorDefinition == null) {
					linter.addMessage(source, "Operator '$valueType $kind ${modifier.type}' hasn't been declared yet.",
						Message.Type.ERROR)
				}
			} catch(error: SignatureResolutionAmbiguityError) {
				//TODO write test for this
				error.log(linter, source, "operator", "$valueType $kind ${modifier.type}")
			}
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		modifier.analyseDataFlow(linter, tracker)
		if(target is VariableValue) {
			tracker.add(listOf(VariableUsage.Type.READ, VariableUsage.Type.MUTATION), target)
		} else {
			target.analyseDataFlow(linter, tracker)
		}
	}
}
