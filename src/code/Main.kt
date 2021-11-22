package code

import objects.Project
import java.nio.file.Files
import java.io.IOException
import java.io.File

object Main {
	const val DEBUG = true

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
        if (!sourceFile.name.endsWith(".pure")) {
            System.err.println("Wrong extension: The provided file is not PURE source code.")
            return
        }
        val sourceCode = try {
            Files.readString(sourceFile.toPath())
        } catch(e: IOException) {
            System.err.println("Failed to read source file.")
            e.printStackTrace()
            return
        }
        println("----- Source code: -----")
        println(sourceCode)
        println("----- Abstract syntax tree: -----")
        val program = ElementGenerator(Project("Main", sourceCode)).parseProgram()
        println(program)
        if(DEBUG) {
            println("----- Intermediate code: -----")
            println(PythonCompiler.compile(InstructionGenerator().generateInstructions(program)))
        }
		println("----- Optimizing: -----")
        val instructions = InstructionGenerator().generateInstructions(program)
        InstructionOptimizer(instructions).optimize()
		println("----- Compiled python code: -----")
        val pythonCode = PythonCompiler.compile(instructions)
        println(pythonCode)
        // Write output file
        println("Creating output file...")
        val targetFileName = "${sourceFile.nameWithoutExtension}.py"
        val targetFile = File(sourceFile.parent, targetFileName)
        targetFile.createNewFile()
        targetFile.printWriter().use { out ->
            out.write(pythonCode)
        }
        println("Done.")
    }

    fun indentText(text: String): String {
        return text.replace("\n", "\n\t")
    }
}