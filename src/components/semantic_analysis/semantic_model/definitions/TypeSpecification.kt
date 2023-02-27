package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import messages.Message
import components.syntax_parser.syntax_tree.definitions.TypeSpecification as TypeSpecificationSyntaxTree

class TypeSpecification(override val source: TypeSpecificationSyntaxTree, scope: Scope, val typeParameters: List<Type>,
						val baseValue: VariableValue): Value(source, scope) {

	init {
		addUnits(baseValue)
		addUnits(typeParameters)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		val baseType = baseValue.type
		if(baseType !is StaticType) {
			linter.addMessage(source, "Type specifications can only be used on initializers.", Message.Type.ERROR)
			return
		}
		baseType.withTypeParameters(typeParameters) { specificType -> type = specificType }
	}

	override fun toString(): String {
		val typeParameterList = typeParameters.joinToString(", ", "<", ">")
		return "$typeParameterList${baseValue.name}"
	}
}
