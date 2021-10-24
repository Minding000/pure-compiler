package elements

import code.Main
import objects.Element
import java.lang.StringBuilder

class Program(val statements: List<Element>): Element() {

    override fun toString(): String {
        val string = StringBuilder()
        for (statement in statements)
            string.append("\n").append(statement.toString())
        return "Program {${Main.indentText(string.toString())}\n}"
    }
}