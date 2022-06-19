package parsing.ast.definitions

import linter.Linter
import linter.elements.general.Unit
import linter.scopes.MutableScope
import parsing.ast.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class GenericsDeclaration(start: Position, private val elements: List<GenericsListElement>): MetaElement(start, elements.last().end) {

    override fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>) {
        for(element in elements)
            element.concretize(linter, scope, units)
    }

    override fun toString(): String {
        return "GenericsDeclaration {${elements.toLines().indent()}\n}"
    }
}