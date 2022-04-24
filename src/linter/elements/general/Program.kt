package linter.elements.general

import linter.Linter
import parsing.ast.general.Program
import java.util.*

class Program(val source: Program) {
	val files = LinkedList<File>()

	/**
	 * Resolves file references by listing files providing types.
	 */
	fun resolveFileReferences(linter: Linter) {
		for(file in files)
			file.resolveFileReferences(linter, this)
	}

	/**
	 * Links types from file references and type aliases.
	 */
	fun linkTypes(linter: Linter) {
		for(file in files)
			file.linkTypes(linter)
	}

	/**
	 * Links type usages to the declaration of the type.
	 */
	fun linkReferences(linter: Linter) {
		for(file in files)
			file.linkReferences(linter)
	}

	/**
	 * Validates various rules including type- and null-safety.
	 */
	fun validate(linter: Linter) {
		for(file in files)
			file.validate(linter)
	}
}