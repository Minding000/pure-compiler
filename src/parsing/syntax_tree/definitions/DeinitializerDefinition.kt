package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.DeinitializerDefinition
import linting.semantic_model.scopes.BlockScope
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.definitions.sections.ModifierSection
import parsing.syntax_tree.definitions.sections.ModifierSectionChild
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.StatementSection
import parsing.tokenizer.WordAtom
import source_structure.Position

class DeinitializerDefinition(start: Position, end: Position, private val body: StatementSection?):
	Element(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	override fun concretize(linter: Linter, scope: MutableScope): DeinitializerDefinition {
		parent?.validate(linter, ALLOWED_MODIFIER_TYPES)
		val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
		val deinitializerScope = BlockScope(scope)
		return DeinitializerDefinition(this, deinitializerScope, body?.concretize(linter, deinitializerScope),
			isNative)
	}

	override fun toString(): String {
		return "Deinitializer { ${body ?: ""} }"
	}
}