package parsing.ast.definitions

import code.Main
import parsing.ast.Element
import source_structure.Position
import java.lang.StringBuilder

class GenericsDeclaration(start: Position, val elements: List<Element>): Element(start, elements.last().end) {

    override fun toString(): String {
        val string = StringBuilder()
        for(element in elements)
            string.append("\n").append(element.toString())
        return "GenericsDeclaration {${Main.indentText(string.toString())}\n}"
    }
}