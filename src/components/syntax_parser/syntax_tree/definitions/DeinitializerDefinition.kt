package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.DeinitializerDefinition as SemanticDeinitializerDefinitionModel
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSection
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSectionChild
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.StatementSection
import components.tokenizer.WordAtom
import source_structure.Position

class DeinitializerDefinition(start: Position, end: Position, private val body: StatementSection?):
	Element(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	override fun concretize(linter: Linter, scope: MutableScope): SemanticDeinitializerDefinitionModel {
		parent?.validate(linter, ALLOWED_MODIFIER_TYPES)
		val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
		val deinitializerScope = BlockScope(scope)
		return SemanticDeinitializerDefinitionModel(this, deinitializerScope,
			body?.concretize(linter, deinitializerScope), isNative)
	}

	override fun toString(): String {
		return "Deinitializer { ${body ?: ""} }"
	}
}
