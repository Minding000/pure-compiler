package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.issues.constant_conditions.TypeSpecificationOutsideOfInitializerCall
import components.syntax_parser.syntax_tree.definitions.TypeSpecification as TypeSpecificationSyntaxTree

class TypeSpecification(override val source: TypeSpecificationSyntaxTree, scope: Scope, val typeParameters: List<Type>,
						val baseValue: VariableValue): Value(source, scope) {

	init {
		addSemanticModels(baseValue)
		addSemanticModels(typeParameters)
	}

	override fun determineTypes() {
		super.determineTypes()
		val baseType = baseValue.type
		if(baseType !is StaticType) {
			context.addIssue(TypeSpecificationOutsideOfInitializerCall(source))
			return
		}
		baseType.withTypeParameters(typeParameters) { specificType -> type = specificType }
	}

	override fun toString(): String {
		val typeParameterList = typeParameters.joinToString(", ", "<", ">")
		return "$typeParameterList${baseValue.name}"
	}
}
