package parsing.ast.definitions

import code.Main
import parsing.ast.Element
import source_structure.Position
import java.lang.StringBuilder

class VariableDeclaration(start: Position, end: Position, val elements: List<Element>): Element(start, end) {

    override fun toString(): String {
        val string = StringBuilder()
        for(element in elements)
            string.append("\n").append(element.toString())
        return "Declaration {${Main.indentText(string.toString())}\n}"
    }
}