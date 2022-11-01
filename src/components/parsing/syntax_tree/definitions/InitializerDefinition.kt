package components.parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.InitializerDefinition as SemanticInitializerDefinitionModel
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.definitions.sections.ModifierSection
import components.parsing.syntax_tree.definitions.sections.ModifierSectionChild
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.StatementSection
import components.tokenizer.WordAtom
import source_structure.Position

class InitializerDefinition(start: Position, private val parameterList: ParameterList?,
							private val body: StatementSection?, end: Position):
	Element(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	override fun concretize(linter: Linter, scope: MutableScope): SemanticInitializerDefinitionModel {
		parent?.validate(linter, ALLOWED_MODIFIER_TYPES)
		val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
		val initializerScope = BlockScope(scope)
		val genericParameters = parameterList?.concretizeGenerics(linter, initializerScope) ?: listOf()
		val parameters = parameterList?.concretizeParameters(linter, initializerScope) ?: listOf()
		return SemanticInitializerDefinitionModel(this, initializerScope, genericParameters, parameters,
			body?.concretize(linter, initializerScope), isNative)
	}

	override fun toString(): String {
		return "Initializer [ ${parameterList ?: ""} ] { ${body ?: ""} }"
	}
}
