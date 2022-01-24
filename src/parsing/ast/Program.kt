package parsing.ast

import source_structure.Section
import util.indent
import util.toLines

class Program(val statements: List<Element>): Section(statements.first().start, statements.last().end) {

    override fun toString(): String {
        return "Program {${statements.toLines().indent()}\n}"
    }
}