package parsing.ast.definitions

import parsing.ast.Element
import parsing.tokenizer.Word
import util.indent
import util.toLines

class VariableDeclaration(val modifierList: ModifierList?, val type: Word, val elements: List<Element>): Element(modifierList?.start ?: type.start, elements.last().end) {

    override fun toString(): String {
        return "VariableDeclaration [${if(modifierList == null) "" else " $modifierList"} ${type.getValue()} ] {${elements.toLines().indent()}\n}"
    }
}