package parsing.ast.definitions

import parsing.ast.general.Element
import parsing.ast.general.MetaElement
import parsing.tokenizer.Word
import util.indent
import util.toLines

class PropertyDeclaration(val modifierList: ModifierList?, val type: Word, val elements: List<Element>): MetaElement(modifierList?.start ?: type.start, elements.last().end) {

    override fun toString(): String {
        return "PropertyDeclaration [${if(modifierList == null) "" else " $modifierList"} ${type.getValue()} ] {${elements.toLines().indent()}\n}"
    }
}