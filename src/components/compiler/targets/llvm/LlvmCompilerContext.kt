package components.compiler.targets.llvm

import components.semantic_analysis.semantic_model.general.Program
import errors.internal.CompilerError
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*

class LlvmCompilerContext(name: String) {
	val context = Llvm.createContext()
	val module = Llvm.createModule(context, name)
	val builder = Llvm.createBuilder(context)
	val i32Type = Llvm.create32BitIntegerType(context)
	val voidType = Llvm.createVoidType(context)

	private var _entrypoint: LLVMValueRef? = null

	var entrypoint: LLVMValueRef
		get() = _entrypoint ?: throw CompilerError("No entrypoint in current build context.")
		set(value) {
			_entrypoint = value
		}

	fun loadSemanticModel(program: Program, entryPointPath: String) {
		program.compile(this)
		entrypoint = program.getEntryPoint(entryPointPath)
	}

	fun verify() {
		val error = BytePointer()
		if(LLVMVerifyModule(module, LLVMPrintMessageAction, error) != Llvm.OK) {
			LLVMDisposeMessage(error)
			throw CompilerError("Failed to compile to LLVM target.")
		}
	}

	fun compile() {
		val passManager = LLVMCreatePassManager()
		LLVMAddAggressiveInstCombinerPass(passManager)
		LLVMAddNewGVNPass(passManager)
		LLVMAddCFGSimplificationPass(passManager)
		LLVMRunPassManager(passManager, module)
		LLVMDisposePassManager(passManager)
	}

	fun printIntermediateRepresentation() {
		LLVMDumpModule(module)
	}

	fun run(): LlvmGenericValue {
		var result: LlvmGenericValue
		LlvmEngine.run(module) { engine ->
			result = Llvm.runFunction(engine, entrypoint)
		}
		return result
	}

	fun close() {
		LLVMDisposeBuilder(builder)
		LLVMContextDispose(context)
	}
}
