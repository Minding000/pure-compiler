package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.scopes.FileScope
import logger.issues.resolution.ReferencedFileNotFound
import java.util.*
import components.syntax_parser.syntax_tree.general.File as FileSyntaxTree
import source_structure.File as SourceFile

class File(override val source: FileSyntaxTree, val file: SourceFile, public override val scope: FileScope): Unit(source, scope) {
	private val referencedFiles = LinkedList<File>()
	val variableTracker = VariableTracker()

	fun resolveFileReferences(linter: Linter, program: Program) {
		for(unit in units) {
			val fileReference = unit as? FileReference ?: continue
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

	override fun linkTypes(linter: Linter) {
		for(referencedFile in referencedFiles)
			scope.reference(referencedFile.scope)
		super.linkTypes(linter)
	}

	fun analyseDataFlow(linter: Linter) {
		analyseDataFlow(linter, variableTracker)
		variableTracker.calculateEndState()
		variableTracker.validate(linter)
	}

//	override fun compile(context: BuildContext): Pointer? {
//		for(unit in units)
//			unit.compile(context)
//		return null
//	}
}
