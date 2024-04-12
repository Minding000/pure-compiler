package components.code_generation.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMExecutionEngineRef
import org.bytedeco.llvm.LLVM.LLVMMCJITCompilerOptions
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object LlvmEngine {
	@Volatile
	private var isInitialized = false

	@Synchronized
	private fun initialize() {
		if(isInitialized)
			return
		LLVMLinkInMCJIT()
		LLVMInitializeNativeAsmPrinter()
		LLVMInitializeNativeAsmParser()
		LLVMInitializeNativeTarget()
		isInitialized = true
	}

	@OptIn(ExperimentalContracts::class)
	fun run(module: LLVMModuleRef, runner: (engine: LLVMExecutionEngineRef) -> Unit) {
		contract {
			callsInPlace(runner, InvocationKind.EXACTLY_ONCE)
		}
		initialize()
		val engine = LLVMExecutionEngineRef()
		val options = LLVMMCJITCompilerOptions()
		val error = BytePointer()
		if(LLVMCreateMCJITCompilerForModule(engine, module, options, 3, error) != Llvm.OK) {
			System.err.println("Failed to create JIT compiler: $error")
			LLVMDisposeMessage(error)
			return
		}
		runner(engine)
		LLVMDisposeExecutionEngine(engine)
	}
}
