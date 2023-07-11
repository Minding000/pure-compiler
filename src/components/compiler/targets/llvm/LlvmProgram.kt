package components.compiler.targets.llvm

import components.semantic_analysis.semantic_model.general.Program
import errors.internal.CompilerError
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*

class LlvmProgram(name: String) {
	val constructor = LlvmConstructor(name)

	private var _entrypoint: LLVMValueRef? = null

	var entrypoint: LLVMValueRef
		get() = _entrypoint ?: throw CompilerError("No entrypoint in current build context.")
		set(value) {
			_entrypoint = value
		}

	fun loadSemanticModel(program: Program, entryPointPath: String? = null) {
		entrypoint = program.compile(constructor, entryPointPath)
	}

	fun verify() {
		val error = BytePointer()
		if(LLVMVerifyModule(constructor.module, LLVMPrintMessageAction, error) != Llvm.OK) {
			LLVMDisposeMessage(error)
			throw CompilerError("Failed to compile to LLVM target.")
		}
	}

	fun compile() {
		val passManager = LLVMCreatePassManager()
		LLVMAddInstructionCombiningPass(passManager)
		LLVMAddNewGVNPass(passManager)
		LLVMAddCFGSimplificationPass(passManager)
		LLVMRunPassManager(passManager, constructor.module)
		LLVMDisposePassManager(passManager)
	}

	fun getIntermediateRepresentation(): String {
		val bytes = LLVMPrintModuleToString(constructor.module)
		val message = bytes.string
		LLVMDisposeMessage(bytes)
		return message
	}

	fun run(): LlvmGenericValue {
		var result: LlvmGenericValue
		LlvmEngine.run(constructor.module) { engine ->
			result = Llvm.runFunction(engine, entrypoint)
		}
		return result
	}

	fun dispose() {
		constructor.dispose()
	}
}
