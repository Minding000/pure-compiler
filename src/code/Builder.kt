package code

import compiler.InstructionGenerator
import compiler.InstructionOptimizer
import compiler.targets.PythonCompiler
import errors.user.UserError
import parsing.ElementGenerator
import source_structure.Module
import source_structure.Project
import java.io.File
import java.io.IOException
import java.nio.file.Files

object Builder {
	private const val LANG_MODULE_PATH = "D:\\Daten\\Projekte\\Pure\\packages\\lang"

	fun build(path: String) {
		try {
			val project = loadProject(path)
			loadRequiredModules(project)
			println("----- Source code: -----")
			println(project)
			println("----- Abstract syntax tree: -----")
			val program = ElementGenerator(project).parseProgram()
			println(program)
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
			addFile(mainModule, source)
		} else {
			project.targetPath = path
			addDirectory(mainModule, source)
		}
		project.addModule(mainModule)
		return project
	}

	fun loadRequiredModules(project: Project) {
		val langModule = Module("Lang")
		addDirectory(langModule, File(LANG_MODULE_PATH))
		project.addModule(langModule)
	}

	private fun addDirectory(module: Module, directory: File) {
		val files = directory.listFiles() ?: throw IOException("Failed to list directory contents of '${directory.name}'.")
		for(file in files) {
			if(file.isFile) {
				addFile(module, file)
			} else {
				addDirectory(module, file)
			}
		}
	}

	private fun addFile(module: Module, file: File) {
		if(!file.name.endsWith(".pure"))
			return
		module.addFile(file.nameWithoutExtension, Files.readString(file.toPath()))
	}
}