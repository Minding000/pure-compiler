package parsing.ast.definitions

import parsing.ast.Element
import parsing.ast.literals.Type

class TypeParameter(val type: Type, val modifier: GenericModifier?): Element(type.start, modifier?.end ?: type.end) {

    override fun toString(): String {
        return "TypeParameter [${if(modifier == null) "" else " $modifier "}] { $type }"
    }
}