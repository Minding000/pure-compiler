package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.general.ErrorHandlingContext
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.Scope
import linting.semantic_model.types.Type
import linting.semantic_model.values.VariableValueDeclaration
import components.parsing.syntax_tree.definitions.GeneratorDefinition

class GeneratorDefinition(override val source: GeneratorDefinition, val scope: BlockScope, name: String,
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
