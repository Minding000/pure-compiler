package components.semantic_model.general

import components.semantic_model.context.VariableTracker
import components.semantic_model.scopes.FileScope
import logger.issues.resolution.ReferencedFileNotFound
import java.util.*
import components.code_generation.llvm.models.general.File as FileUnit
import components.syntax_parser.syntax_tree.general.File as FileSyntaxTree
import source_structure.File as SourceFile

class File(override val source: FileSyntaxTree, val file: SourceFile, override val scope: FileScope, val statements: List<SemanticModel>):
	SemanticModel(source, scope) {
	private val referencedFiles = LinkedList<File>()
	lateinit var variableTracker: VariableTracker

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

	override fun toUnit() = FileUnit(this)
}
