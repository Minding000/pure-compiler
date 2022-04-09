package parsing.ast.definitions

import parsing.ast.general.MetaElement
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type

class DeclarationList(private val identifiers: List<Identifier>, private val type: Type): MetaElement(identifiers.first().start, type.end) {

	override fun toString(): String {
		return "DeclarationList { ${identifiers.joinToString()}: $type }"
	}
}