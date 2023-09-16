package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.scopes.BlockScope
import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSection
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSectionChild
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.tokenizer.WordAtom
import source_structure.Position
import components.semantic_model.declarations.InitializerDefinition as SemanticInitializerDefinitionModel

class InitializerDefinition(start: Position, private val parameterList: ParameterList?, private val body: StatementSection?, end: Position):
	SyntaxTreeNode(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.CONVERTING, WordAtom.NATIVE, WordAtom.OVERRIDING)
	}

	override fun toSemanticModel(scope: MutableScope): SemanticInitializerDefinitionModel {
		parent?.validate(ALLOWED_MODIFIER_TYPES)
		val isAbstract = parent?.containsModifier(WordAtom.ABSTRACT) ?: false
		val isConverting = parent?.containsModifier(WordAtom.CONVERTING) ?: false
		val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
		val isOverriding = parent?.containsModifier(WordAtom.OVERRIDING) ?: false
		val initializerScope = BlockScope(scope)
		val genericParameters = parameterList?.getSemanticGenericParameterModels(initializerScope) ?: emptyList()
		val parameters = parameterList?.getSemanticParameterModels(initializerScope) ?: emptyList()
		return SemanticInitializerDefinitionModel(this, initializerScope, genericParameters, parameters,
			body?.toSemanticModel(initializerScope), isAbstract, isConverting, isNative, isOverriding)
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
