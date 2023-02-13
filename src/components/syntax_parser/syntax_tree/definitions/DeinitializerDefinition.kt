package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSection
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSectionChild
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.StatementSection
import components.tokenizer.WordAtom
import source_structure.Position
import components.semantic_analysis.semantic_model.definitions.DeinitializerDefinition as SemanticDeinitializerDefinitionModel

class DeinitializerDefinition(start: Position, end: Position, private val body: StatementSection?):
	Element(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	override fun concretize(linter: Linter, scope: MutableScope): SemanticDeinitializerDefinitionModel {
		parent?.validate(linter, ALLOWED_MODIFIER_TYPES)
		val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
		return SemanticDeinitializerDefinitionModel(this, body?.concretize(linter, scope), isNative)
	}

	override fun toString(): String {
		var stringRepresentation = "Deinitializer"
		if(body != null)
			stringRepresentation += " { $body }"
		return stringRepresentation
	}
}
