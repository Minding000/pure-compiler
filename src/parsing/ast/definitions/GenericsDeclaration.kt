package parsing.ast.definitions

import parsing.ast.general.Element
import parsing.ast.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class GenericsDeclaration(start: Position, private val elements: List<Element>): MetaElement(start, elements.last().end) {

    override fun toString(): String {
        return "GenericsDeclaration {${elements.toLines().indent()}\n}"
    }
}