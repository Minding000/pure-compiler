package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSection
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSectionChild
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.StatementSection
import components.tokenizer.WordAtom
import errors.internal.CompilerError
import source_structure.Position
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition as SemanticInitializerDefinitionModel

class InitializerDefinition(start: Position, private val parameterList: ParameterList?, private val body: StatementSection?, end: Position):
	Element(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.CONVERTING, WordAtom.NATIVE, WordAtom.OVERRIDING)
	}

	override fun concretize(linter: Linter, scope: MutableScope): SemanticInitializerDefinitionModel {
		parent?.validate(linter, ALLOWED_MODIFIER_TYPES)
		val surroundingTypeDefinition = scope.getSurroundingDefinition()
			?: throw CompilerError("Initializer expected surrounding type definition.")
		val isAbstract = parent?.containsModifier(WordAtom.ABSTRACT) ?: false
		val isConverting = parent?.containsModifier(WordAtom.CONVERTING) ?: false
		val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
		val isOverriding = parent?.containsModifier(WordAtom.OVERRIDING) ?: false
		val initializerScope = BlockScope(scope)
		val genericParameters = parameterList?.concretizeGenerics(linter, initializerScope) ?: listOf()
		val parameters = parameterList?.concretizeParameters(linter, initializerScope) ?: listOf()
		return SemanticInitializerDefinitionModel(this, surroundingTypeDefinition, initializerScope, genericParameters, parameters,
			body?.concretize(linter, initializerScope), isAbstract, isConverting, isNative, isOverriding)
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
