package parsing.ast

import code.Main
import source_structure.Section
import java.lang.StringBuilder

class Program(val statements: List<Element>): Section(statements.first().start, statements.last().end) {

    override fun toString(): String {
        val string = StringBuilder()
        for(statement in statements)
            string.append("\n").append(statement.toString())
        return "Program {${Main.indentText(string.toString())}\n}"
    }
}