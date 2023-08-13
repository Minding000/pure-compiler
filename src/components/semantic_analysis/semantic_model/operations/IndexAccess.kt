package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.declarations.FunctionSignature
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.access.IndexAccess as IndexAccessSyntaxTree

class IndexAccess(override val source: IndexAccessSyntaxTree, scope: Scope, val target: Value, val typeParameters: List<Type>,
				  val indices: List<Value>): Value(source, scope) {
	var sourceExpression: Value? = null
	var targetSignature: FunctionSignature? = null

	init {
		addSemanticModels(typeParameters, indices)
		addSemanticModels(target)
	}

	override fun determineTypes() {
		determineSourceExpression()
		super.determineTypes()
		target.type?.let { targetType ->
			try {
				targetSignature = targetType.interfaceScope.getIndexOperator(typeParameters, indices, sourceExpression)
				if(targetSignature == null) {
					val name = "${target.type}[${indices.joinToString { index -> index.type.toString() }}]"
					context.addIssue(NotFound(source, "Operator", "$name(${sourceExpression?.type ?: ""})"))
					return@let
				}
				type = targetSignature?.returnType
			} catch(error: SignatureResolutionAmbiguityError) {
				error.log(source, "operator", getSignature(targetType))
			}
		}
	}

	private fun determineSourceExpression() {
		val parent = parent
		if(parent is Assignment && parent.targets.contains(this))
			sourceExpression = parent.sourceExpression
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

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		staticValue = this
	}

	fun filterForPossibleTargetTypes(availableTypes: List<ObjectType>): List<ObjectType> {
		return availableTypes.filter { availableType ->
			availableType.interfaceScope.getIndexOperator(typeParameters, indices, sourceExpression) != null
		}
	}
}
