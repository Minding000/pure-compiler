package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition as SemanticInitializerDefinitionModel
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSection
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSectionChild
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.StatementSection
import components.tokenizer.WordAtom
import errors.user.IncorrectContextError
import messages.Message
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
		val surroundingTypeDefinition = scope.getSurroundingDefinition()
		if(surroundingTypeDefinition == null)
			throw IncorrectContextError("Initializers are only allowed inside of classes or enums.") //TODO write test for this
		val initializerScope = BlockScope(scope)
		val genericParameters = parameterList?.concretizeGenerics(linter, initializerScope) ?: listOf()
		val parameters = parameterList?.concretizeParameters(linter, initializerScope) ?: listOf()
		return SemanticInitializerDefinitionModel(this, surroundingTypeDefinition, initializerScope,
			genericParameters, parameters, body?.concretize(linter, initializerScope), isNative)
	}

	override fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>) {
		try {
			super.concretize(linter, scope, units)
		} catch(error: IncorrectContextError) {
			linter.addMessage(this, error.message, Message.Type.ERROR)
		}
	}

	override fun toString(): String {
		return "Initializer [ ${parameterList ?: ""} ] { ${body ?: ""} }"
	}
}
