package parsing.ast.literals

import linter.Linter
import linter.elements.literals.Type
import linter.scopes.MutableScope
import parsing.ast.general.MetaElement
import parsing.ast.general.TypeElement
import source_structure.Position
import util.indent
import util.toLines
import java.util.*

class TypeList(private val typeParameters: List<TypeElement>, start: Position, end: Position): MetaElement(start, end) {

	fun concretizeTypes(linter: Linter, scope: MutableScope): List<Type> {
		val types = LinkedList<Type>()
		for(type in typeParameters)
			types.add(type.concretize(linter, scope))
		return types
	}

	override fun toString(): String {
		return "TypeList {${typeParameters.toLines().indent()}\n}"
	}
}