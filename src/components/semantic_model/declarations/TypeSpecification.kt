package components.semantic_model.declarations

import components.semantic_model.scopes.Scope
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import logger.issues.constant_conditions.TypeSpecificationOutsideOfInitializerCall
import components.syntax_parser.syntax_tree.definitions.TypeSpecification as TypeSpecificationSyntaxTree

class TypeSpecification(override val source: TypeSpecificationSyntaxTree, scope: Scope, val globalTypes: List<Type>,
						val baseValue: VariableValue): Value(source, scope) {

	init {
		addSemanticModels(baseValue)
		addSemanticModels(globalTypes)
	}

	override fun determineTypes() {
		super.determineTypes()
		val baseType = baseValue.providedType
		if(baseType !is StaticType) {
			context.addIssue(TypeSpecificationOutsideOfInitializerCall(source))
			return
		}
		providedType = baseType //TODO actually return <TypeParameters>StaticType here (similar to how ObjectTypes handle type parameters)
	}

	override fun toString(): String {
		val globalTypeList = globalTypes.joinToString(", ", "<", ">")
		return "$globalTypeList${baseValue.name}"
	}
}
