package linter.elements.general

import linter.Linter
import linter.messages.Message
import linter.scopes.FileScope
import parsing.ast.general.File as AstFile
import source_structure.File as SourceFile
import java.util.*

class File(val source: AstFile, val file: SourceFile, val scope: FileScope): Unit() {
	val referencedFiles = LinkedList<File>()

	fun resolveFileReferences(linter: Linter, program: Program) {
		for(unit in units) {
			if(unit is FileReference) {
				var noFilesFound = true
				for(file in program.files) {
					if(file == this)
						continue
					if(file.matches(unit.parts)) {
						referencedFiles.add(file)
						noFilesFound = false
						linter.messages.add(Message("'${file.file.name}' referenced in '${this.file.name}' ${unit.parts}.", Message.Type.DEBUG))
					}
				}
				if(noFilesFound)
					linter.messages.add(Message("Failed to resolve file '${unit.identifier}'.", Message.Type.ERROR))
			}
		}
	}

	fun matches(parts: List<String>): Boolean {
		if(parts.size > file.pathParts.size + 1)
			return false
		for(p in parts.indices) {
			if(p == file.pathParts.size)
				return parts[p] == file.name
			if(parts[p] != file.pathParts[p])
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