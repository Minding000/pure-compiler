package code

import components.code_generation.llvm.LlvmCompiler
import components.semantic_model.context.SemanticModelGenerator
import components.syntax_parser.element_generator.SyntaxTreeGenerator
import errors.internal.CompilerError
import errors.user.UserError
import logger.Severity
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

	fun run(path: String, entryPointPath: String) {
		try {
			val project = loadProject(path)
			loadRequiredModules(project)
			if(PRINT_SOURCE_CODE) {
				println("----- Source code: -----")
				println(project)
			}
			val abstractSyntaxTree = SyntaxTreeGenerator(project).parseProgram()
			if(PRINT_AST) {
				println("----- Abstract syntax tree: -----")
				println(abstractSyntaxTree)
			}
			val semanticModelGenerator = SemanticModelGenerator(project.context)
			val semanticModel = semanticModelGenerator.createSemanticModel(abstractSyntaxTree)
			project.context.logger.printReport(Severity.INFO)
			println("----- JIT output: -----")
			LlvmCompiler.buildAndRun(project, semanticModel, entryPointPath)
			println("Done.")
		} catch(e: UserError) {
			println("Failed to compile: ${e.message}")
			if(Main.DEBUG)
				e.printStackTrace()
		}
	}

	fun build(path: String, entryPointPath: String) {
		try {
			val project = loadProject(path)
			loadRequiredModules(project)
			if(PRINT_SOURCE_CODE) {
				println("----- Source code: -----")
				println(project)
			}
			val abstractSyntaxTree = SyntaxTreeGenerator(project).parseProgram()
			if(PRINT_AST) {
				println("----- Abstract syntax tree: -----")
				println(abstractSyntaxTree)
			}
			val semanticModelGenerator = SemanticModelGenerator(project.context)
			val semanticModel = semanticModelGenerator.createSemanticModel(abstractSyntaxTree)
			project.context.logger.printReport(Severity.INFO)
			println("----- Build output: -----")
			LlvmCompiler.build(project, semanticModel, entryPointPath)
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
