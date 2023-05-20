package code

import components.compiler.targets.llvm.LLVMIRCompiler
import components.semantic_analysis.semantic_model.context.SemanticModelGenerator
import components.syntax_parser.element_generator.SyntaxTreeGenerator
import errors.internal.CompilerError
import errors.user.UserError
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
			val program = SyntaxTreeGenerator(project).parseProgram()
			if(PRINT_AST) {
				println("----- Abstract syntax tree: -----")
				println(program)
			}
			println("----- Linter messages: -----")
			val semanticModelGenerator = SemanticModelGenerator(project.context)
			val lintedProgram = semanticModelGenerator.createSemanticModel(program)
			project.context.logger.printReport()
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
		try {
			val source = File(path)
			val project = Project(source.nameWithoutExtension)
			val mainModule = Module(project, "Main")
			if(source.isFile) {
				project.targetPath = source.parent
				addFile(mainModule, LinkedList(), source)
			} else {
				project.targetPath = path
				addDirectory(mainModule, LinkedList(), source)
			}
			project.addModule(mainModule)
			return project
		} catch(cause: IOException) {
			throw CompilerError("Failed to load project.", cause)
		}
	}

	fun loadRequiredModules(project: Project) {
		val langModule = Module(project, "Pure")
		val path = System.getenv("BASE_MODULE_PATH") ?: LANG_MODULE_PATH
		addDirectory(langModule, LinkedList(), File(path))
		project.addModule(langModule)
	}

	private fun addDirectory(module: Module, parts: List<String>, directory: File) {
		val files = directory.listFiles()
			?: throw IOException("Failed to list directory contents of '${directory.name}' at '${directory.path}'.")
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
