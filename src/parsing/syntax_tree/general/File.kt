package parsing.syntax_tree.general

import linting.Linter
import linting.semantic_model.general.File
import source_structure.File as SourceFile
import linting.semantic_model.scopes.FileScope
import linting.semantic_model.scopes.MutableScope
import source_structure.Position
import util.indent
import util.toLines

class File(start: Position, end: Position, private val file: SourceFile, private val statements: List<Element>): Element(start, end) {

    override fun concretize(linter: Linter, scope: MutableScope): File {
        val file = File(this, file, scope as FileScope)
        for(statement in statements)
            statement.concretize(linter, scope, file.units)
        return file
    }

    override fun toString(): String {
        return "File {${statements.toLines().indent()}\n}"
    }
}