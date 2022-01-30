package parsing.ast.general

import parsing.ast.Element
import parsing.ast.literals.Identifier
import source_structure.Position
import util.indent
import util.toLines

class FileReference(start: Position, val files: List<Identifier>, val body: AliasBlock?): Element(start, body?.end ?: files.last().end) {

	override fun toString(): String {
		return "FileReference {${"${files.toLines()}${if(body != null) "\n$body" else ""}".indent()}\n}"
	}
}