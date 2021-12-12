package source_structure

import code.Main
import java.lang.StringBuilder
import java.util.*

class Project(val name: String) {
    lateinit var targetPath: String
    val modules = LinkedList<Module>()

    fun addModule(module: Module) {
        modules.add(module)
    }

    override fun toString(): String {
        val string = StringBuilder()
        for(module in modules)
            string.append("\n").append(module.toString())
        return "Program [$name] {${Main.indentText(string.toString())}\n}"
    }
}