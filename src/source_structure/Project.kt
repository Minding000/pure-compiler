package source_structure

import components.semantic_model.context.Context
import util.indent
import util.toLines
import java.io.File
import java.util.*

class Project(val name: String) {
	val context = Context()
	lateinit var targetPath: String
	val modules = LinkedList<Module>()
	val outputPath = ".${File.separator}out"

	fun addModule(module: Module) {
		modules.add(module)
	}

	override fun toString(): String {
		return "Program [$name] {${modules.toLines().indent()}\n}"
	}
}
