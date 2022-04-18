package parsing.ast.general

import linter.Linter
import linter.elements.general.File
import source_structure.File as SourceFile
import linter.scopes.FileScope
import linter.scopes.Scope
import source_structure.Position
import util.indent
import util.toLines

class File(start: Position, end: Position, private val file: SourceFile, private val statements: List<Element>): Element(start, end) {

    override fun concretize(linter: Linter, scope: Scope): File {
        val file = File(this, file, scope as FileScope)
        for(statement in statements)
            statement.concretize(linter, scope, file.units)
        return file
    }

    override fun toString(): String {
        return "File {${statements.toLines().indent()}\n}"
    }
}