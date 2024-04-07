package code

import components.code_generation.llvm.LlvmCompiler
import components.semantic_model.context.SemanticModelGenerator
import components.semantic_model.context.SpecialType
import components.semantic_model.general.Program
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
	val PRINT_SUBJECTS = listOf("source", "ast", "llvm-ir")
	val specialTypePaths = mapOf(
		SpecialType.ARRAY to listOf("Pure", "lang", "collections", "Array"),
		SpecialType.STRING to listOf("Pure", "lang", "dataTypes", "String"),
		SpecialType.BYTE to listOf("Pure", "lang", "dataTypes", "Byte"),
		SpecialType.INTEGER to listOf("Pure", "lang", "dataTypes", "Int"),
		SpecialType.FLOAT to listOf("Pure", "lang", "dataTypes", "Float"),
		SpecialType.BOOLEAN to listOf("Pure", "lang", "dataTypes", "Bool"),
		SpecialType.NULL to listOf("Pure", "lang", "dataTypes", "Null"),
		SpecialType.FUNCTION to listOf("Pure", "lang", "dataTypes", "Function"),
		SpecialType.ITERABLE to listOf("Pure", "lang", "collections", "Iterable"),
		SpecialType.INDEX_ITERATOR to listOf("Pure", "lang", "collections", "iterators", "IndexIterator"),
		SpecialType.KEY_ITERATOR to listOf("Pure", "lang", "collections", "iterators", "KeyIterator"),
		SpecialType.VALUE_ITERATOR to listOf("Pure", "lang", "collections", "iterators", "ValueIterator"),
		SpecialType.NEVER to listOf("Pure", "lang", "dataTypes", "Never"),
		SpecialType.NOTHING to listOf("Pure", "lang", "dataTypes", "Nothing"),
		SpecialType.IDENTIFIABLE to listOf("Pure", "lang", "dataTypes", "Identifiable"),
		SpecialType.ANY to listOf("Pure", "lang", "dataTypes", "Any"),
	)

	fun run(path: String, entryPointPath: String) {
		try {
			val project = loadProject(path)
			val semanticModel = createSemanticModel(project)
			println("----- JIT output: -----")
			LlvmCompiler.buildAndRun(project, semanticModel, entryPointPath)
		} catch(error: UserError) {
			System.err.println("Failed to compile: ${error.message}")
			if(Main.shouldPrintCompileTimeDebugOutput)
				error.printStackTrace()
		}
	}

	fun build(path: String, entryPointPath: String) {
		try {
			val project = loadProject(path)
			val semanticModel = createSemanticModel(project)
			println("----- Build output: -----")
			LlvmCompiler.build(project, semanticModel, entryPointPath)
		} catch(error: UserError) {
			System.err.println("Failed to compile: ${error.message}")
			if(Main.shouldPrintCompileTimeDebugOutput)
				error.printStackTrace()
		}
	}

	fun print(subject: String, path: String, entryPointPath: String) {
		val project = loadProject(path)
		loadRequiredModules(project)
		if(subject == "source") {
			println(project)
			return
		}
		val abstractSyntaxTree = SyntaxTreeGenerator(project).parseProgram()
		if(subject == "ast") {
			println(abstractSyntaxTree)
			return
		}
		val semanticModelGenerator = SemanticModelGenerator(project.context)
		val semanticModel = semanticModelGenerator.createSemanticModel(abstractSyntaxTree, specialTypePaths)
		val intermediateRepresentation = LlvmCompiler.getIntermediateRepresentation(project, semanticModel, entryPointPath)
		println(intermediateRepresentation)
	}

	private fun createSemanticModel(project: Project): Program {
		loadRequiredModules(project)
		val abstractSyntaxTree = SyntaxTreeGenerator(project).parseProgram()
		val semanticModelGenerator = SemanticModelGenerator(project.context)
		val semanticModel = semanticModelGenerator.createSemanticModel(abstractSyntaxTree, specialTypePaths)
		project.context.logger.printReport(Main.logLevel)
		return semanticModel
	}

	private fun loadProject(path: String): Project {
		try {
			val source = File(path)
			val project = Project(source.nameWithoutExtension)
			val projectModule = Module(project, project.name)
			if(source.isFile) {
				project.targetPath = source.parent
				addFile(projectModule, LinkedList(), source)
			} else {
				project.targetPath = path
				val files = source.listFiles()
					?: throw IOException("Failed to list directory contents of '${source.name}' at '${source.path}'.")
				for(file in files) {
					if(file.isFile)
						addFile(projectModule, LinkedList(), file)
					else
						addDirectory(projectModule, LinkedList(), file)
				}
			}
			project.addModule(projectModule)
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
			if(file.isFile)
				addFile(module, childPathParts, file)
			else
				addDirectory(module, childPathParts, file)
		}
	}

	private fun addFile(module: Module, pathParts: List<String>, file: File) {
		if(!file.name.endsWith(".pure"))
			return
		module.addFile(pathParts, file.nameWithoutExtension, Files.readString(file.toPath()))
	}
}
