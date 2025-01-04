package components.code_generation.llvm.wrapper

import components.semantic_model.general.Program
import source_structure.Project
import java.io.File

/**
 * @see: https://github.com/bytedeco/javacpp-presets/tree/master/llvm
 */
object LlvmCompiler {

	fun getIntermediateRepresentation(project: Project, semanticModel: Program, entryPointPath: String): String {
		val program = LlvmProgram(project.name)
		try {
			program.loadSemanticModel(semanticModel, entryPointPath)
			val intermediateRepresentation = program.getIntermediateRepresentation()
			return intermediateRepresentation
		} finally {
			program.dispose()
		}
	}

	fun build(project: Project, semanticModel: Program, entryPointPath: String): LlvmProgram {
		val program = LlvmProgram(project.name)
		try {
			program.loadSemanticModel(semanticModel, entryPointPath)
			program.verify()
			program.compile()
			program.writeObjectFileTo("${project.outputPath}${File.separator}program.o")
		} finally {
			program.dispose()
		}
		return program
	}

	fun buildAndRun(project: Project, semanticModel: Program, entryPointPath: String) {
		val program = LlvmProgram(project.name)
		try {
			program.loadSemanticModel(semanticModel, entryPointPath)
			program.verify()
			program.compile()
			println("----- JIT output: -----")
			val result = program.runAndReturnInt()
			println("Exit code: $result")
		} finally {
			program.dispose()
		}
	}
}
