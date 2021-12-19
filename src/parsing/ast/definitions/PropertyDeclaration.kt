package parsing.ast.definitions

import code.Main
import parsing.ast.Element
import source_structure.Position
import java.lang.StringBuilder

class PropertyDeclaration(start: Position, end: Position, val modifierList: ModifierList?, val elements: List<Element>): Element(start, end) {

    override fun toString(): String {
        val string = StringBuilder()
        for(element in elements)
            string.append("\n").append(element.toString())
        return "PropertyDeclaration [${if(modifierList == null) "" else "$modifierList "}] {${Main.indentText(string.toString())}\n}"
    }
}