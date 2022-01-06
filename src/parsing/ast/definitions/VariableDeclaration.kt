package parsing.ast.definitions

import code.Main
import parsing.ast.Element
import parsing.tokenizer.Word
import source_structure.Position
import java.lang.StringBuilder

class VariableDeclaration(val modifierList: ModifierList?, val type: Word, val elements: List<Element>): Element(modifierList?.start ?: type.start, elements.last().end) {

    override fun toString(): String {
        val string = StringBuilder()
        for(element in elements)
            string.append("\n").append(element.toString())
        return "VariableDeclaration [${if(modifierList == null) "" else " $modifierList"} ${type.getValue()} ] {${Main.indentText(string.toString())}\n}"
    }
}