package components.code_generation.llvm.wrapper

import org.bytedeco.javacpp.Pointer
import org.bytedeco.llvm.global.LLVM
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
		LLVM.LLVMInitializeNativeTarget()
		LLVM.LLVMInitializeNativeAsmPrinter()
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
		val threadSafeModule = constructor.toThreadSafeModule()
		val jit = LlvmOrc.createJit()
		val library = LlvmOrc.getMainLibrary(jit)
		LlvmOrc.addModuleToLibrary(jit, library, threadSafeModule)
		val function = LlvmOrc.lookupInMainLibrary(jit, functionName)
		runner(function)
		LLVM.LLVMOrcDisposeLLJIT(jit)
		LLVM.LLVMShutdown()
	}
}
