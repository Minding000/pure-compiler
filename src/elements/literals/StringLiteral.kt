package elements.literals

import objects.Element

class StringLiteral(val value: String): Element() {

    override fun toString(): String {
        return "StringLiteral { \"$value\" }"
    }
}