package linting.semantic_model.general

import parsing.syntax_tree.general.FileReference

class FileReference(val source: FileReference, val identifier: String, val parts: List<String>, val aliases: List<ReferenceAlias>): Unit() {

	init {
		units.addAll(aliases)
	}
}