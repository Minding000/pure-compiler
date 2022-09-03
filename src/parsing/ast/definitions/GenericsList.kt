package parsing.ast.definitions

import linter.Linter
import linter.elements.values.TypeDefinition
import linter.scopes.MutableScope
import parsing.ast.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines
import java.util.*

class GenericsList(start: Position, val elements: List<GenericsListElement>, end: Position): MetaElement(start, end) {

	fun concretizeGenerics(linter: Linter, scope: MutableScope): List<TypeDefinition> {
		val generics = LinkedList<TypeDefinition>()
		for(element in this.elements)
			generics.add(element.concretize(linter, scope))
		return generics
	}

	override fun toString(): String {
		return "GenericsList {${elements.toLines().indent()}\n}"
	}
}