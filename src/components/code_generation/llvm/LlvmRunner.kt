package components.code_generation.llvm

import errors.internal.CompilerError
import org.bytedeco.javacpp.LongPointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.llvm.LLVM.LLVMOrcLLJITRef
import org.bytedeco.llvm.global.LLVM.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object LlvmRunner {
	@Volatile
	private var isInitialized = false

	@Synchronized
	private fun initialize() {
		if(isInitialized)
			return
		LLVMInitializeNativeAsmPrinter()
		LLVMInitializeNativeTarget()
		isInitialized = true
	}

	/**
	 * see: https://github.com/bytedeco/javacpp-presets/blob/master/llvm/samples/llvm/OrcJit.java
	 */
	@OptIn(ExperimentalContracts::class)
	fun run(constructor: LlvmConstructor, functionName: String, runner: (address: Pointer) -> Unit) {
		contract {
			callsInPlace(runner, InvocationKind.EXACTLY_ONCE)
		}
		initialize()
		//LLVMDumpModule(constructor.module) //TODO correct bytedeco example
		val threadSafeModule = constructor.toThreadSafeModule()
		val jit = LLVMOrcLLJITRef()
		val jitBuilder = LLVMOrcCreateLLJITBuilder()
		//Loader.loadGlobal(Loader.load(LLVM::class.java)) //TODO correct bytedeco example
		var error = LLVMOrcCreateLLJIT(jit, jitBuilder)
		if(error != null) {
			val errorMessage = LLVMGetErrorMessage(error)
			val message = "Failed to create JIT compiler: ${errorMessage.string}"
			LLVMDisposeErrorMessage(errorMessage)
			throw CompilerError(message)
		}
		val mainDylib = LLVMOrcLLJITGetMainJITDylib(jit)
		error = LLVMOrcLLJITAddLLVMIRModule(jit, mainDylib, threadSafeModule)
		if(error != null) {
			val errorMessage = LLVMGetErrorMessage(error)
			val message = "Failed to add module to JIT: ${errorMessage.string}"
			LLVMDisposeErrorMessage(errorMessage)
			throw CompilerError(message)
		}
		val entrypoint = LongPointer(1)
		error = LLVMOrcLLJITLookup(jit, entrypoint, functionName)
		if(error != null) {
			val errorMessage = LLVMGetErrorMessage(error)
			val message = "Failed to find entrypoint '$functionName' in JIT: ${errorMessage.string}"
			LLVMDisposeErrorMessage(errorMessage) //TODO correct bytedeco example
			throw CompilerError(message)
		}
		val function = object: Pointer() {
			init {
				address = entrypoint.get()
			}
		}
		runner(function)
		LLVMOrcDisposeLLJIT(jit)
		LLVMShutdown()
	}
}
