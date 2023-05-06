package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.scopes.FileScope
import logger.issues.resolution.ReferencedFileNotFound
import java.util.*
import components.syntax_parser.syntax_tree.general.File as FileSyntaxTree
import source_structure.File as SourceFile

class File(override val source: FileSyntaxTree, val file: SourceFile, override val scope: FileScope, val statements: List<Unit>):
	Unit(source, scope) {
	private val referencedFiles = LinkedList<File>()
	lateinit var variableTracker: VariableTracker

	init {
		addUnits(statements)
	}

	fun resolveFileReferences(linter: Linter, program: Program) {
		for(statement in statements) {
			val fileReference = statement as? FileReference ?: continue
			var noFilesFound = true
			for(file in program.files) {
				if(file == this)
					continue
				if(file.matches(fileReference.parts)) {
					referencedFiles.add(file)
					noFilesFound = false
				}
			}
			if(noFilesFound)
				linter.addIssue(ReferencedFileNotFound(fileReference))
		}
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

	override fun declare(linter: Linter) {
		super.declare(linter)
		for(referencedFile in referencedFiles)
			scope.reference(referencedFile.scope)
	}

	fun analyseDataFlow(linter: Linter) {
		variableTracker = VariableTracker(linter)
		analyseDataFlow(variableTracker)
		variableTracker.calculateEndState()
		variableTracker.validate()
	}

//	override fun compile(context: BuildContext): Pointer? {
//		for(statement in statements)
//			statement.compile(context)
//		return null
//	}
}
