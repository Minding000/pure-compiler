package code

import compiler.targets.llvm.LLVMIRCompiler
import errors.user.UserError
import linter.Linter
import parsing.element_generator.ElementGenerator
import source_structure.Module
import source_structure.Project
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*

object Builder {
	private const val LANG_MODULE_PATH = "D:\\Daten\\Projekte\\Pure\\packages\\lang"
	private const val PRINT_SOURCE_CODE = false
	private const val PRINT_AST = false

	fun build(path: String) {
		try {
			val project = loadProject(path)
			loadRequiredModules(project)
			if(PRINT_SOURCE_CODE) {
				println("----- Source code: -----")
				println(project)
			}
			val program = ElementGenerator(project).parseProgram()
			if(PRINT_AST) {
				println("----- Abstract syntax tree: -----")
				println(program)
			}
			println("----- Linter messages: -----")
			val linter = Linter()
			val lintedProgram = linter.lint(program)
			linter.printMessages()
			println("----- JIT example: -----")
			LLVMIRCompiler.runExampleProgram()
			println("----- JIT output: -----")
			LLVMIRCompiler.compile(lintedProgram)
			/*
			if(Main.DEBUG) {
				println("----- Intermediate code: -----")
				println(PythonCompiler.compile(InstructionGenerator().generateInstructions(program), true))
			}
			println("----- Optimizing: -----")
			val instructions = InstructionGenerator().generateInstructions(program)
			InstructionOptimizer(instructions).optimize()
			println("----- Compiled python code: -----")
			val pythonCode = PythonCompiler.compile(instructions)
			println(pythonCode)
			println("-----")
			// Write output file
			println("Creating output file...")
			val targetFileName = "${project.name}.py"
			val targetFile = File(project.targetPath, targetFileName)
			targetFile.createNewFile()
			targetFile.printWriter().use { out ->
				out.write(pythonCode)
			}
			*/
			println("Done.")
		} catch(e: UserError) {
			println("Failed to compile: ${e.message}")
			if(Main.DEBUG)
				e.printStackTrace()
		}
	}

	private fun loadProject(path: String): Project {
		val source = File(path)
		val project = Project(source.nameWithoutExtension)
		val mainModule = Module("Main")
		if(source.isFile) {
			project.targetPath = source.parent
			addFile(mainModule, LinkedList(), source)
		} else {
			project.targetPath = path
			addDirectory(mainModule, LinkedList(), source)
		}
		project.addModule(mainModule)
		return project
	}

	private fun loadRequiredModules(project: Project) {
		val langModule = Module("Lang")
		addDirectory(langModule, LinkedList(), File(LANG_MODULE_PATH))
		project.addModule(langModule)
	}

	private fun addDirectory(module: Module, parts: List<String>, directory: File) {
		val files = directory.listFiles() ?: throw IOException("Failed to list directory contents of '${directory.name}'.")
		val childPathParts = LinkedList(parts)
		childPathParts.add(directory.name)
		for(file in files) {
			if(file.isFile) {
				addFile(module, childPathParts, file)
			} else {
				addDirectory(module, childPathParts, file)
			}
		}
	}

	private fun addFile(module: Module, pathParts: List<String>, file: File) {
		if(!file.name.endsWith(".pure"))
			return
		module.addFile(pathParts, file.nameWithoutExtension, Files.readString(file.toPath()))
	}
}