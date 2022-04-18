package parsing.ast.operations

import parsing.ast.general.Element
import parsing.ast.general.MetaElement
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type
import util.indent

class Cast(val value: Element, val operator: String, val identifier: Identifier?, val type: Type): MetaElement(value.start, type.end) {

	override fun toString(): String {
		return "Cast {${"\n$value $operator ${if(identifier == null) "" else "$identifier: "}$type".indent()}\n}"
	}
}