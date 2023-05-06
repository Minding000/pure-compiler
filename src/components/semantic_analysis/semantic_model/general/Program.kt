package components.semantic_analysis.semantic_model.general

import components.compiler.targets.llvm.BuildContext
import components.semantic_analysis.Linter
import java.util.*
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree

class Program(val source: ProgramSyntaxTree) {
	val files = LinkedList<File>()

	fun getFile(pathParts: List<String>): File? {
		for(file in files)
			if(file.matches(pathParts))
				return file
		return null
	}

	/**
	 * Resolves file references by listing files providing types.
	 */
	fun resolveFileReferences(linter: Linter) {
		for(file in files)
			file.resolveFileReferences(linter, this)
	}

	/**
	 * Declares types and values.
	 */
	fun declare(linter: Linter) {
		for(file in files)
			file.declare(linter)
	}

	/**
	 * Determines the type of values.
	 */
	fun determineTypes(linter: Linter) {
		for(file in files)
			file.determineTypes(linter)
	}

	/**
	 * Collects information about variable usage order.
	 */
	fun analyseDataFlow(linter: Linter) {
		for(file in files)
			file.analyseDataFlow(linter)
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
