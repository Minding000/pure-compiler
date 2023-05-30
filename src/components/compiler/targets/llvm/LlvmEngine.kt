package components.compiler.targets.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMExecutionEngineRef
import org.bytedeco.llvm.LLVM.LLVMMCJITCompilerOptions
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.*

object LlvmEngine {
	private var isInitialized = false

	private fun initialize() {
		if(isInitialized)
			return
		LLVMInitializeCore(LLVMGetGlobalPassRegistry())
		LLVMLinkInMCJIT()
		LLVMInitializeNativeAsmPrinter()
		LLVMInitializeNativeAsmParser()
		LLVMInitializeNativeTarget()
		isInitialized = true
	}

	fun run(module: LLVMModuleRef, runner: (engine: LLVMExecutionEngineRef) -> Unit) {
		initialize()
		val engine = LLVMExecutionEngineRef()
		val options = LLVMMCJITCompilerOptions()
		val error = BytePointer()
		if(LLVMCreateMCJITCompilerForModule(engine, module, options, 3, error) != LlvmCompiler.LLVM_OK) {
			System.err.println("Failed to create JIT compiler: $error")
			LLVMDisposeMessage(error)
			return
		}
		runner(engine)
		LLVMDisposeExecutionEngine(engine)
	}
}
