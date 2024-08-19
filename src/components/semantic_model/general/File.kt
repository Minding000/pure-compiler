package components.semantic_model.general

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmFunction
import components.semantic_model.context.VariableTracker
import components.semantic_model.scopes.FileScope
import logger.issues.resolution.ReferencedFileNotFound
import java.util.*
import components.syntax_parser.syntax_tree.general.File as FileSyntaxTree
import source_structure.File as SourceFile

class File(override val source: FileSyntaxTree, val file: SourceFile, override val scope: FileScope, val statements: List<SemanticModel>):
	SemanticModel(source, scope) {
	private val referencedFiles = LinkedList<File>()
	lateinit var variableTracker: VariableTracker
	lateinit var initializer: LlvmFunction
	lateinit var runner: LlvmFunction

	init {
		addSemanticModels(statements)
	}

	fun matches(parts: List<String>): Boolean {
		if(parts.size > file.pathParts.size + 1)
			return false
		for(partIndex in parts.indices) {
			if(partIndex == file.pathParts.size)
				return parts[partIndex] == file.name
			if(parts[partIndex] != file.pathParts[partIndex])
				return false
		}
		return true
	}

	fun resolveFileReferences(program: Program) {
		for(statement in statements) {
			val fileReference = statement as? FileReference ?: continue
			var noFilesFound = true
			for(file in program.files) {
				if(file == this)
					continue
				if(file.matches(fileReference.parts)) {
					referencedFiles.add(file)
					scope.reference(file.scope, fileReference.getNameAliases())
					noFilesFound = false
				}
			}
			if(noFilesFound)
				context.addIssue(ReferencedFileNotFound(fileReference))
		}
	}

	fun analyseDataFlow() {
		variableTracker = VariableTracker(context)
		analyseDataFlow(variableTracker)
		variableTracker.calculateEndState()
		variableTracker.validate()
	}

	override fun declare(constructor: LlvmConstructor) {
		super.declare(constructor)
		initializer = LlvmFunction(constructor, "${file.name}_FileInitializer", listOf(constructor.pointerType))
		runner = LlvmFunction(constructor, "${file.name}_FileRunner", listOf(constructor.pointerType))
	}

	override fun compile(constructor: LlvmConstructor) {
		compileInitializer(constructor)
		compileRunner(constructor)
	}

	private fun compileInitializer(constructor: LlvmConstructor) {
		constructor.createAndSelectEntrypointBlock(initializer.value)
		context.printDebugLine(constructor, "Initializing file '${file.name}'.")
		val exceptionParameter = context.getExceptionParameter(constructor)
		for(typeDeclaration in scope.typeDeclarations.values) {
			if(typeDeclaration.isDefinition) {
				constructor.buildFunctionCall(typeDeclaration.classInitializer, listOf(exceptionParameter))
				context.continueRaise(constructor, this)
			}
		}
		context.printDebugLine(constructor, "File '${file.name}' initialized.")
		constructor.buildReturn()
	}

	private fun compileRunner(constructor: LlvmConstructor) {
		constructor.createAndSelectEntrypointBlock(runner.value)
		context.printDebugLine(constructor, "Executing file '${file.name}'.")
		super.compile(constructor)
		context.printDebugLine(constructor, "File '${file.name}' executed.")
		constructor.buildReturn()
	}
}
