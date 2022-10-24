package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.InitializerDefinition
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.definitions.sections.ModifierSection
import parsing.syntax_tree.definitions.sections.ModifierSectionChild
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.StatementSection
import parsing.tokenizer.WordAtom
import source_structure.Position

class InitializerDefinition(start: Position, private val parameterList: ParameterList?,
							private val body: StatementSection?, end: Position):
	Element(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	override fun concretize(linter: Linter, scope: MutableScope): InitializerDefinition {
		parent?.validate(linter, ALLOWED_MODIFIER_TYPES)
		val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
		val initializerScope = BlockScope(scope)
		val genericParameters = parameterList?.concretizeGenerics(linter, initializerScope) ?: listOf()
		val parameters = parameterList?.concretizeParameters(linter, initializerScope) ?: listOf()
		return InitializerDefinition(this, initializerScope, genericParameters, parameters,
			body?.concretize(linter, initializerScope), isNative)
	}

	override fun toString(): String {
		return "Initializer [ ${parameterList ?: ""} ] { ${body ?: ""} }"
	}
}
