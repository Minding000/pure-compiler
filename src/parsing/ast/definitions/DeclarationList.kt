package parsing.ast.definitions

import parsing.ast.general.MetaElement
import parsing.ast.literals.Identifier
import parsing.ast.general.TypeElement

class DeclarationList(private val identifiers: List<Identifier>, private val type: TypeElement): MetaElement(identifiers.first().start, type.end) {

	override fun toString(): String {
		return "DeclarationList { ${identifiers.joinToString()}: $type }"
	}
}