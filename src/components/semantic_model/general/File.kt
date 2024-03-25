package components.semantic_model.general

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.code_generation.llvm.LlvmValue
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
	lateinit var llvmInitializerValue: LlvmValue
	lateinit var llvmInitializerType: LlvmType

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
		llvmInitializerType = constructor.buildFunctionType(listOf(constructor.pointerType))
		llvmInitializerValue = constructor.buildFunction("${file.name}_FileInitializer", llvmInitializerType)
	}

	override fun compile(constructor: LlvmConstructor) {
		constructor.createAndSelectEntrypointBlock(llvmInitializerValue)
		for(typeDeclaration in scope.typeDeclarations.values) {
			if(typeDeclaration.isDefinition) {
				constructor.buildFunctionCall(typeDeclaration.llvmClassInitializerType, typeDeclaration.llvmClassInitializer,
					listOf(context.getExceptionParameter(constructor)))
				context.continueRaise(constructor)
			}
		}
		super.compile(constructor)
		constructor.buildReturn()
	}
}
