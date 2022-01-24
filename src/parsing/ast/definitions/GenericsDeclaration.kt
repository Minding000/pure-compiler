package parsing.ast.definitions

import parsing.ast.Element
import source_structure.Position
import util.indent
import util.toLines

class GenericsDeclaration(start: Position, val elements: List<Element>): Element(start, elements.last().end) {

    override fun toString(): String {
        return "GenericsDeclaration {${elements.toLines().indent()}\n}"
    }
}