package components.code_generation.llvm

import components.semantic_model.general.Program
import source_structure.Project

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

	fun build(project: Project, semanticModel: Program, entryPointPath: String) {
		val program = LlvmProgram(project.name)
		try {
			program.loadSemanticModel(semanticModel, entryPointPath)
			program.verify()
			program.compile()
			program.writeObjectFileTo("${project.outputPath}\\program.o")
		} finally {
			program.dispose()
		}
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
