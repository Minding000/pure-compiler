package elements.literals

import objects.Element

class NumberLiteral(val value: Int): Element() {

    override fun toString(): String {
        return "NumberLiteral { $value }"
    }
}