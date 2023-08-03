package components.semantic_analysis.semantic_model.operations

import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.Operator
import components.semantic_analysis.semantic_model.values.Value
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.access.IndexAccess as IndexAccessSyntaxTree

class IndexAccess(override val source: IndexAccessSyntaxTree, scope: Scope, val target: Value, val typeParameters: List<Type>,
				  val indices: List<Value>): Value(source, scope) {
	var sourceExpression: Value? = null
	var implementation: FunctionImplementation? = null

	init {
		addSemanticModels(typeParameters, indices)
		addSemanticModels(target)
	}

	override fun determineTypes() {
		val parent = parent
		if(parent is Assignment && parent.targets.contains(this))
			sourceExpression = parent.sourceExpression
		super.determineTypes()
		target.type?.let { targetType ->
			try {
				val signature = targetType.interfaceScope.resolveIndexOperator(typeParameters, indices, sourceExpression)
				if(signature == null) {
					val name = "${target.type}[${indices.joinToString { index -> index.type.toString() }}]"
					context.addIssue(NotFound(source, "Operator", "$name(${sourceExpression?.type ?: ""})"))
					return@let
				}
				type = signature.returnType
				val kind = if(sourceExpression == null) Operator.Kind.BRACKETS_GET else Operator.Kind.BRACKETS_SET
				val property = targetType.interfaceScope.resolveValue(kind.stringRepresentation) as? PropertyDeclaration
				val function = property?.value as? Function
				implementation = function?.getImplementationBySignature(signature)
			} catch(error: SignatureResolutionAmbiguityError) {
				error.log(source, "operator", getSignature(targetType))
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		staticValue = this
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

	fun filterForPossibleTargetTypes(availableTypes: List<ObjectType>): List<ObjectType> {
		return availableTypes.filter { availableType ->
			availableType.interfaceScope.resolveIndexOperator(typeParameters, indices, sourceExpression) != null
		}
	}
}
