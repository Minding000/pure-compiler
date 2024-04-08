package components.code_generation.llvm

import components.semantic_model.general.Program
import source_structure.Project

/**
 * @see: https://github.com/bytedeco/javacpp-presets/tree/master/llvm
 */
object LlvmCompiler {

	fun getIntermediateRepresentation(project: Project, semanticModel: Program, entryPointPath: String): String {
		val program = LlvmProgram(project.name)
		program.loadSemanticModel(semanticModel, entryPointPath)
		val intermediateRepresentation = program.getIntermediateRepresentation()
		program.dispose()
		return intermediateRepresentation
	}

	fun build(project: Project, semanticModel: Program, entryPointPath: String) {
		val program = LlvmProgram(project.name)
		program.loadSemanticModel(semanticModel, entryPointPath)
		program.verify()
		program.compile()
		program.writeTo(".\\out")
		program.dispose()
	}

	fun buildAndRun(project: Project, semanticModel: Program, entryPointPath: String) {
		val program = LlvmProgram(project.name)
		program.loadSemanticModel(semanticModel, entryPointPath)
		program.verify()
		program.compile()
		println("----- JIT output: -----")
		val result = program.run()
		val intResult = Llvm.castToSignedInteger(result)
		println("Result: '${intResult}'")
		program.dispose()
	}
}
