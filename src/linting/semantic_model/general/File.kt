package linting.semantic_model.general

import linting.Linter
import messages.Message
import linting.semantic_model.scopes.FileScope
import parsing.syntax_tree.general.File as FileSyntaxTree
import source_structure.File as SourceFile
import java.util.*

class File(override val source: FileSyntaxTree, val file: SourceFile, val scope: FileScope): Unit(source) {
	private val referencedFiles = LinkedList<File>()

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
					linter.addMessage("'${file.file.name}' referenced in '${this.file.name}' by ${fileReference.parts}.", Message.Type.DEBUG)
				}
			}
			if(noFilesFound)
				linter.addMessage("Failed to resolve file '${fileReference.identifier}'.", Message.Type.ERROR)
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

	fun linkTypes(linter: Linter) {
		for(referencedFile in referencedFiles)
			scope.reference(referencedFile.scope)
		linkTypes(linter, scope)
	}

	fun linkPropertyParameters(linter: Linter) {
		linkPropertyParameters(linter, scope)
	}

	fun linkValues(linter: Linter) {
		linkValues(linter, scope)
	}

//	override fun compile(context: BuildContext): Pointer? {
//		for(unit in units)
//			unit.compile(context)
//		return null
//	}
}
