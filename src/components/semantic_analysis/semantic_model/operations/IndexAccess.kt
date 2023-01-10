package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import errors.user.SignatureResolutionAmbiguityError
import messages.Message
import components.syntax_parser.syntax_tree.access.IndexAccess as IndexAccessSyntaxTree

class IndexAccess(override val source: IndexAccessSyntaxTree, val target: Value, val typeParameters: List<Type>,
				  val indices: List<Value>): Value(source) {
	var sourceExpression: Value? = null

	init {
		addUnits(target)
		addUnits(typeParameters, indices)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		target.type?.let { targetType ->
			try {
				val definition = targetType.scope.resolveIndexOperator(typeParameters, indices, sourceExpression)
				if(definition == null) {
					val name = "${target.type}[${indices.joinToString { index -> index.type.toString() }}]"
					linter.addMessage(source,
						"Operator '$name(${sourceExpression?.type ?: ""})' hasn't been declared yet.",
						Message.Type.ERROR)
					return@let
				}
				type = definition.returnType
			} catch(error: SignatureResolutionAmbiguityError) {
				error.log(linter, source, "operator", getSignature(targetType))
			}
		}
	}

	private fun getSignature(targetType: Type): String {
		var signature = "$targetType["
		if(typeParameters.isNotEmpty()) {
			signature += typeParameters.joinToString()
			signature += ";"
			if(indices.isNotEmpty())
				signature += " "
		}
		signature += indices.joinToString { index -> index.type.toString() }
		signature += "]"
		sourceExpression?.let { sourceExpression ->
			signature += "(${sourceExpression.type})"
		}
		return signature
	}
}
