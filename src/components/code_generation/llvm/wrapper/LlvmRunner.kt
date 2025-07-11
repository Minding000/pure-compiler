package components.code_generation.llvm.wrapper

import errors.internal.CompilerError
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.llvm.LLVM.LLVMMemoryBufferRef
import org.bytedeco.llvm.global.LLVM
import org.bytedeco.llvm.global.LLVM.LLVMOrcLLJITAddObjectFile
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
	fun run(constructor: LlvmConstructor, functionName: String, libraryPaths: List<String> = emptyList(),
			runner: (address: Pointer) -> Unit) {
		contract {
			callsInPlace(runner, InvocationKind.EXACTLY_ONCE)
		}
		initialize()
		val threadSafeModule = constructor.toThreadSafeModule()
		val jit = LlvmOrc.createJit()
		val library = LlvmOrc.getMainLibrary(jit)
		for(libraryPath in libraryPaths)
			addLibrary(jit, library, libraryPath)
		LlvmOrc.addModuleToLibrary(jit, library, threadSafeModule)
		val function = LlvmOrc.lookupInMainLibrary(jit, functionName)
		runner(function)
		LLVM.LLVMOrcDisposeLLJIT(jit)
		LLVM.LLVMShutdown()
	}

	private fun addLibrary(jit: OrcJit, library: OrcLibrary, path: String) {
		val memoryBuffer = LLVMMemoryBufferRef()
		val pathPointer = BytePointer(path)
		val readError = BytePointer()
		if(LLVM.LLVMCreateMemoryBufferWithContentsOfFile(pathPointer, memoryBuffer, readError) != Llvm.OK)
			throw CompilerError("Failed read library: ${Llvm.getMessage(readError)}")
		val loadError = LLVMOrcLLJITAddObjectFile(jit, library, memoryBuffer)
		Llvm.handleError(loadError, "Failed load library")
	}
}
