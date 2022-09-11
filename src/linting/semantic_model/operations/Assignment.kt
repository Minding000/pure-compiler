package linting.semantic_model.operations

import linting.Linter
import linting.semantic_model.access.Index
import linting.semantic_model.access.MemberAccess
import linting.semantic_model.general.Unit
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValue
import linting.messages.Message
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.operations.Assignment

class Assignment(val source: Assignment, private val targets: List<Value>, private val sourceExpression: Value):
	Unit() {

	init {
		units.addAll(targets)
		units.add(sourceExpression)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		verifyAssignability(linter)
		sourceExpression.type?.let {
			for(target in targets) {
				val targetType = target.type
				if(targetType == null) {
					target.type = it
				} else {
					if(!it.isAssignableTo(targetType))
						linter.messages.add(Message("${target.source.getStartString()}: " +
								"Type '$it' is not assignable to type '$targetType'.", Message.Type.ERROR))
				}
			}
		}
	}

	private fun verifyAssignability(linter: Linter) {
		for(target in targets) {
			when(target) {
				is VariableValue -> {
					if(target.definition?.isConstant == true)
						linter.messages.add(Message("${target.source.getStartString()}: " +
								"'${target.name}' cannot be reassigned, because it is constant.", Message.Type.ERROR))
				}
				is MemberAccess -> {
					if(target.member.definition?.isConstant == true)
						linter.messages.add(Message("${target.source.getStartString()}: " +
								"'${target.member.name}' cannot be reassigned, because it is constant.", Message.Type.ERROR))
				}
				is Index -> {
					target.target.type?.let { targetType ->
						targetType.scope.resolveIndexOperator(target.indices.map { index -> index.type },
							listOf(sourceExpression.type))
					}
				}
				else -> {
					linter.messages.add(Message("${target.source.getStartString()}: " +
							"Expression is not assignable.", Message.Type.ERROR))
				}
			}
		}
	}
}