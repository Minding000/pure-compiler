package components.semantic_model.general

import components.code_generation.llvm.models.general.Program
import components.semantic_model.context.Context
import java.util.*
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree

class Program(val context: Context, val source: ProgramSyntaxTree) {
	val files = LinkedList<File>()

	companion object {
		const val GLOBAL_ENTRYPOINT_NAME = "main"
		const val RUNTIME_PREFIX = "pure_runtime_"
	}

	fun getFile(pathParts: List<String>): File? {
		for(file in files)
			if(file.matches(pathParts))
				return file
		return null
	}

	/**
	 * Declares types and values.
	 */
	fun declare() {
		for(file in files)
			file.declare()
	}

	/**
	 * Resolves file references by listing files providing types.
	 */
	fun resolveFileReferences() {
		for(file in files)
			file.resolveFileReferences(this)
	}

	/**
	 * Determines the type of values.
	 */
	fun determineTypes() {
		for(file in files)
			file.determineTypes()
	}

	/**
	 * Collects information about variable usage order.
	 */
	fun analyseDataFlow() {
		for(file in files)
			file.analyseDataFlow()
	}

	/**
	 * Validates various rules including type- and null-safety.
	 */
	fun validate() {
		for(file in files)
			file.validate()
	}

	/**
	 * Creates a LLVM representation of the program.
	 */
	fun toUnit(): Program {
		return Program(context, this, files.map(File::toUnit))
	}
}
