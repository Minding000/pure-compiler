package linter.elements.operations

import linter.Linter
import linter.elements.general.Unit
import linter.elements.values.Value
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.operations.Assignment

class Assignment(val source: Assignment, val targets: List<Value>, val sourceExpression: Value): Unit() {

	init {
		units.addAll(targets)
		units.add(sourceExpression)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, scope)
		sourceExpression.type?.let {
			for(target in targets) {
				val targetType = target.type
				if(targetType == null) {
					target.type = it
				} else {
					if(!it.isAssignableTo(targetType))
						linter.messages.add(Message("${source.getStartString()}: Type '$it' is not assignable to type '$targetType'.", Message.Type.ERROR))
				}
			}
		}
	}
}