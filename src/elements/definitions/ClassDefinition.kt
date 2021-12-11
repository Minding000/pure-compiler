package elements.definitions

import code.Main
import elements.identifier.ClassIdentifier
import elements.Element
import elements.VoidElement
import scopes.ClassScope
import source_structure.Position
import java.lang.StringBuilder

class ClassDefinition(start: Position, end: Position, val identifier: ClassIdentifier, val subScope: ClassScope, val members: List<Element>): VoidElement(start, end) {

	override fun toString(): String {
		val string = StringBuilder()
		for(member in members)
			string.append("\n").append(member.toString())
		return "Class [$identifier] {${Main.indentText(string.toString())}\n}"
	}
}