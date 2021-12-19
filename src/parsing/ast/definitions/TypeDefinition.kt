package parsing.ast.definitions

import parsing.ast.Element
import parsing.ast.literals.Identifier

class TypeDefinition(val modifierList: ModifierList?, val type: TypeType, val identifier: Identifier, val body: TypeBody): Element(modifierList?.start ?: type.start, body.end) {

	override fun toString(): String {
		return "TypeDefinition [${if(modifierList == null) "" else "$modifierList "}$type $identifier] { $body }"
	}
}