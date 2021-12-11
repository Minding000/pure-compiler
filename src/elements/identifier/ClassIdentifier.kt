package elements.identifier

import elements.VoidElement
import elements.definitions.ClassDefinition
import scopes.Scope
import word_generation.Word

open class ClassIdentifier(override val parentScope: Scope, word: Word): VoidElement(word.start, word.end), Identifier {
	override val name = getValue()
	lateinit var definition: ClassDefinition

	override fun serializeDeclarationPosition(): String {
		return getRegionString()
	}

	override fun toString(): String {
		return "ClassIdentifier { ${getValue()} }"
	}
}