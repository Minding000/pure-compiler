package components.semantic_analysis.semantic_model.general

import components.compiler.targets.llvm.LlvmCompilerContext
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.Object
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Function
import errors.user.UserError
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
	 * Compiles code to LLVM IR.
	 */
	fun compile(llvmCompilerContext: LlvmCompilerContext) {
		for(file in files)
			file.compile(llvmCompilerContext)
	}

	fun getEntryPoint(entryPointPath: String): FunctionImplementation {
		val pathSections = entryPointPath.split(":")
		if(pathSections.size != 2)
			throw UserError("Malformed entry point path '$entryPointPath'.")
		val (filePath, functionPath) = pathSections
		val file = getFile(filePath.split(".")) ?: throw UserError("File '$filePath' not found.")
		val functionPathParts = functionPath.split(".").toMutableList()
		val functionName = functionPathParts.removeLast()
		var scope: Scope = file.scope
		for(objectName in functionPathParts) {
			val objectDefinition = scope.resolveType(objectName) as? Object ?: throw UserError("Object '$objectName' not found.")
			if(objectDefinition.isBound)
				throw UserError("Object '$objectName' is bound.")
			scope = objectDefinition.scope
		}
		val functionVariable = scope.resolveValue(functionName) ?: throw UserError("Function '$functionName' not found.")
		val function = functionVariable.value as? Function ?: throw UserError("Variable '$functionName' is not a function.")
		val functionImplementation = function.implementations.find { functionImplementation ->
			functionImplementation.signature.takesNoParameters() } ?: throw UserError("Function '$functionName' has no overload without parameters.")
		return functionImplementation
	}
}
