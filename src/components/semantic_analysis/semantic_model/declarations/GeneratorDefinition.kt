package components.semantic_analysis.semantic_model.declarations

import components.semantic_analysis.semantic_model.general.ErrorHandlingContext
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import java.util.*
import components.syntax_parser.syntax_tree.definitions.GeneratorDefinition as GeneratorDefinitionSyntaxTree

class GeneratorDefinition(override val source: GeneratorDefinitionSyntaxTree, override val scope: BlockScope, name: String,
						  val parameters: List<Parameter>, val keyReturnType: Type?, val valueReturnType: Type,
						  val body: ErrorHandlingContext, isSpecificCopy: Boolean = false):
	ValueDeclaration(source, scope, name, isSpecificCopy = isSpecificCopy) {

	init {
		addSemanticModels(keyReturnType, valueReturnType, body)
		addSemanticModels(parameters)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): GeneratorDefinition {
		val specificParameters = LinkedList<Parameter>()
		for(parameter in parameters)
			specificParameters.add(parameter.withTypeSubstitutions(typeSubstitutions))
		return GeneratorDefinition(source, scope, name, specificParameters, keyReturnType?.withTypeSubstitutions(typeSubstitutions),
			valueReturnType.withTypeSubstitutions(typeSubstitutions), body)
	}
}
