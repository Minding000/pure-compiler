package source_structure

import util.indent
import util.toLines
import java.util.*

class Project(val name: String) {
    lateinit var targetPath: String
    val modules = LinkedList<Module>()

    fun addModule(module: Module) {
        modules.add(module)
    }

    override fun toString(): String {
        return "Program [$name] {${modules.toLines().indent()}\n}"
    }
}