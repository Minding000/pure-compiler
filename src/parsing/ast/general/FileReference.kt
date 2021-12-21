package parsing.ast.general

import parsing.ast.Element
import source_structure.Position

class FileReference(start: Position, val file: Element, val body: AliasBlock?): Element(start, body?.end ?: file.end) {

	override fun toString(): String {
		return "FileReference { $file${if(body != null) " $body" else ""} }"
	}
}