package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.operations.MemberAccess
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import errors.internal.CompilerError
import messages.Message
import components.syntax_parser.syntax_tree.definitions.TypeSpecification as TypeSpecificationSyntaxTree

class TypeSpecification(override val source: TypeSpecificationSyntaxTree, val baseValue: Value,
						val typeParameters: List<Type>): Value(source) {

	init {
		addUnits(baseValue)
		addUnits(typeParameters)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		val baseType = baseValue.type
		if(baseType !is StaticType) {
			linter.addMessage(source, "Type specifications can only be used on initializers.",
				Message.Type.WARNING) //TODO write test for this
			return
		}
		baseType.withTypeParameters(typeParameters) { specificType ->
			type = specificType
		}
	}

	override fun toString(): String {
		val typeParameterList = typeParameters.joinToString(", ", "<", ">")
		return when(baseValue) {
			is VariableValue -> "$typeParameterList${baseValue.name}"
			is MemberAccess -> "${baseValue.target.type}.$typeParameterList${baseValue.member.name}"
			else -> throw CompilerError("Type specification can only contain VariableValues or MemberAccesses, " +
				"but found '${baseValue.javaClass.simpleName}'.")
		}
	}
}
