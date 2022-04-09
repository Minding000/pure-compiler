package linter.elements.general

import linter.Linter
import parsing.ast.general.Program
import java.util.*

class Program(val source: Program) {
	val files = LinkedList<File>()

	fun resolveFileReferences(linter: Linter) {
		for(file in files)
			file.resolveFileReferences(linter, this)
	}

	fun linkTypes(linter: Linter) {
		for(file in files)
			file.linkTypes(linter)
	}

	fun linkReferences(linter: Linter) {
		for(file in files)
			file.linkReferences(linter)
	}

	fun validate(linter: Linter) {
		for(file in files)
			file.validate(linter)
	}
}