package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.DeinitializerDefinition
import linter.scopes.BlockScope
import linter.scopes.Scope
import parsing.ast.definitions.sections.ModifierSection
import parsing.ast.definitions.sections.ModifierSectionChild
import parsing.ast.general.Element
import parsing.ast.general.StatementSection
import parsing.tokenizer.WordAtom
import source_structure.Position

class DeinitializerDefinition(start: Position, end: Position, private val body: StatementSection?):
	Element(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	override fun concretize(linter: Linter, scope: Scope): DeinitializerDefinition {
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