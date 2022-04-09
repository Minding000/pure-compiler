package linter.elements.general

import parsing.ast.general.FileReference

class FileReference(val source: FileReference, val identifier: String, val parts: List<String>, val aliases: List<ReferenceAlias>): Unit() {

	init {
		units.addAll(aliases)
	}
}