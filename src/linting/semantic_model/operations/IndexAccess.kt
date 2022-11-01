package linting.semantic_model.operations

import linting.Linter
import linting.semantic_model.scopes.Scope
import linting.semantic_model.types.Type
import linting.semantic_model.values.Value
import messages.Message
import components.parsing.syntax_tree.access.IndexAccess as IndexAccessSyntaxTree

class IndexAccess(override val source: IndexAccessSyntaxTree, val target: Value, val typeParameters: List<Type>,
				  val indices: List<Value>): Value(source) {
	var sourceExpression: Value? = null

	init {
		units.add(target)
		units.addAll(typeParameters)
		units.addAll(indices)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		target.type?.let { targetType ->
			val definition = targetType.scope.resolveIndexOperator(typeParameters, indices, sourceExpression)
			if(definition == null) {
				val name = "${target.type}[${indices.joinToString { index -> index.type.toString() }}]"
				linter.addMessage(source,
					"Operator '$name(${sourceExpression?.type ?: ""})' hasn't been declared yet.",
					Message.Type.ERROR)
				return@let
			}
			type = definition.returnType
		}
	}
}
