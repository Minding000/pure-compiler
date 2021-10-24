package code

import java.nio.file.Files
import java.io.IOException
import java.io.File

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            println("Nothing to compile.")
            return
        }
        if (args.size > 1) {
            println("To many arguments (only 1 file required).")
            return
        }
        val sourceFile = File(args.first())
        val sourceFileName = sourceFile.name
        if (!sourceFileName.endsWith(".pure")) {
            System.err.println("Wrong extension: The provided file is not PURE source code.")
            return
        }
        val sourceCode = try {
            Files.readString(sourceFile.toPath())
        } catch (e: IOException) {
            System.err.println("Failed to read source file.")
            e.printStackTrace()
            return
        }
        println(sourceCode)
        println(ElementGenerator(sourceCode).parseProgram())
    }

    fun indentText(text: String): String {
        return "\t" + text.replace("\n", "\n\t")
    }
}