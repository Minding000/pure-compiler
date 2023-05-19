package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSection
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSectionChild
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.StatementSection
import components.tokenizer.WordAtom
import source_structure.Position
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition as SemanticInitializerDefinitionModel

class InitializerDefinition(start: Position, private val parameterList: ParameterList?, private val body: StatementSection?, end: Position):
	Element(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.CONVERTING, WordAtom.NATIVE, WordAtom.OVERRIDING)
	}

	override fun concretize(scope: MutableScope): SemanticInitializerDefinitionModel {
		parent?.validate(ALLOWED_MODIFIER_TYPES)
		val isAbstract = parent?.containsModifier(WordAtom.ABSTRACT) ?: false
		val isConverting = parent?.containsModifier(WordAtom.CONVERTING) ?: false
		val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
		val isOverriding = parent?.containsModifier(WordAtom.OVERRIDING) ?: false
		val initializerScope = BlockScope(scope)
		val genericParameters = parameterList?.concretizeGenerics(initializerScope) ?: listOf()
		val parameters = parameterList?.concretizeParameters(initializerScope) ?: listOf()
		return SemanticInitializerDefinitionModel(this, initializerScope, genericParameters, parameters,
			body?.concretize(initializerScope), isAbstract, isConverting, isNative, isOverriding)
	}

	override fun toString(): String {
		var stringRepresentation = "Initializer"
		if(parameterList != null)
			stringRepresentation += " [ $parameterList ]"
		if(body != null)
			stringRepresentation += " { $body }"
		return stringRepresentation
	}
}
