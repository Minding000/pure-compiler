package parsing.ast.definitions

import code.Main
import parsing.ast.Element
import parsing.ast.literals.Identifier

class TypeDefinition(val type: TypeType, val identifier: Identifier, val body: TypeBody): Element(type.start, body.end) {

	override fun toString(): String {
		return "TypeDefinition [$type $identifier] {${Main.indentText("\n${body}")}\n}"
	}
}