package linter.elements.general

import compiler.targets.llvm.BuildContext
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
	 * Links type usages to their declaration.
	 */
	fun linkTypes(linter: Linter) {
		for(file in files)
			file.linkTypes(linter)
	}

	/**
	 * Links properties in initializer parameters.
	 */
	fun linkPropertyParameters(linter: Linter) {
		for(file in files)
			file.linkPropertyParameters(linter)
	}

	/**
	 * Links value usages to their declaration.
	 */
	fun linkValues(linter: Linter) {
		for(file in files)
			file.linkValues(linter)
	}

	/**
	 * Validates various rules including type- and null-safety.
	 */
	fun validate(linter: Linter) {
		for(file in files)
			file.validate(linter)
	}

	/**
	 * Compiles code to LLVM IR
	 */
	fun compile(context: BuildContext) {
//		for(file in files)
//			file.compile(context)
	}
}