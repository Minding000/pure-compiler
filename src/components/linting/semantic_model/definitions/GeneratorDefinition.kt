package components.linting.semantic_model.definitions

import components.linting.Linter
import components.linting.semantic_model.general.ErrorHandlingContext
import components.linting.semantic_model.scopes.BlockScope
import components.linting.semantic_model.scopes.Scope
import components.linting.semantic_model.types.Type
import components.linting.semantic_model.values.VariableValueDeclaration
import components.parsing.syntax_tree.definitions.GeneratorDefinition as GeneratorDefinitionSyntaxTree

class GeneratorDefinition(override val source: GeneratorDefinitionSyntaxTree, val scope: BlockScope, name: String,
						  val parameters: List<Parameter>, val keyReturnType: Type?, val valueReturnType: Type,
						  val body: ErrorHandlingContext): VariableValueDeclaration(source, name) {

	init {
		units.addAll(parameters)
		if(keyReturnType != null)
			units.add(keyReturnType)
		units.add(valueReturnType)
		units.add(body)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, this.scope)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, this.scope)
	}
}
