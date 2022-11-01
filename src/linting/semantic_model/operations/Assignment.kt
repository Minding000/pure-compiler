package linting.semantic_model.operations

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import messages.Message
import linting.semantic_model.scopes.Scope
import components.parsing.syntax_tree.operations.Assignment

class Assignment(override val source: Assignment, private val targets: List<Value>,
				 private val sourceExpression: Value): Unit(source) {

	init {
		units.addAll(targets)
		units.add(sourceExpression)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		sourceExpression.linkValues(linter, scope)
		for(target in targets) {
			if(target is IndexAccess)
				target.sourceExpression = sourceExpression
			target.linkValues(linter, scope)
		}
		verifyAssignability(linter)
		for(target in targets) {
			val targetType = target.type
			if(sourceExpression.isAssignableTo(targetType)) {
				sourceExpression.setInferredType(targetType)
			} else if(targetType == null) {
				target.type = sourceExpression.type
			} else {
				sourceExpression.type?.let { sourceType ->
					linter.addMessage(target.source,
						"Type '$sourceType' is not assignable to type '$targetType'.", Message.Type.ERROR)
				}
			}
		}
	}

	private fun verifyAssignability(linter: Linter) {
		for(target in targets) {
			when(target) {
				is VariableValue -> {
					if(target.definition?.isConstant == true)
						linter.addMessage(target.source,
							"'${target.name}' cannot be reassigned, because it is constant.",
							Message.Type.ERROR)
				}
				is MemberAccess -> {
					if(target.member.definition?.isConstant == true)
						linter.addMessage(target.source,
							"'${target.member.name}' cannot be reassigned, because it is constant.",
							Message.Type.ERROR)
				}
				is IndexAccess -> {
					target.target.type?.scope?.resolveIndexOperator(target.typeParameters, target.indices,
						sourceExpression)
				}
				else -> {
					linter.addMessage(target.source, "Expression is not assignable.", Message.Type.ERROR)
				}
			}
		}
	}
}
