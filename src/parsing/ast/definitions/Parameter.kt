package parsing.ast.definitions

import parsing.ast.Element

class Parameter(val modifierList: ModifierList?, val identifier: TypedIdentifier): Element(modifierList?.start ?: identifier.start, identifier.end) {

    override fun toString(): String {
        return "Parameter [${if(modifierList == null) "" else " $modifierList "}] { $identifier }"
    }
}