package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.DeinitializerDefinition
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.definitions.sections.ModifierSection
import parsing.ast.definitions.sections.ModifierSectionChild
import parsing.ast.general.Element
import parsing.ast.general.StatementSection
import source_structure.Position

class DeinitializerDefinition(start: Position, end: Position, private val body: StatementSection?):
	Element(start, end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	override fun concretize(linter: Linter, scope: Scope): DeinitializerDefinition {
		var isNative = false
		parent?.let {
			for(modifier in it.getModifiers(linter)) {
				when(val name = modifier.getValue()) {
					"native" -> isNative = true
					else -> linter.messages.add(Message("Modifier '$name' is not applicable to deinitializers."))
				}
			}
		}
		return DeinitializerDefinition(this, body?.concretize(linter, scope), isNative)
	}

	override fun toString(): String {
		return "Deinitializer { ${body ?: ""} }"
	}
}