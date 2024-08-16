package components.code_generation.llvm.wrapper

import errors.internal.CompilerError
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.LongPointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.llvm.LLVM.LLVMOrcCJITDylibSearchOrderElement
import org.bytedeco.llvm.LLVM.LLVMOrcCLookupSetElement
import org.bytedeco.llvm.LLVM.LLVMOrcCSymbolMapPair
import org.bytedeco.llvm.LLVM.LLVMOrcExecutionSessionLookupHandleResultFunction
import org.bytedeco.llvm.global.LLVM.*
import java.util.concurrent.CompletableFuture

@Suppress("MemberVisibilityCanBePrivate", "unused")
object LlvmOrc {

	fun createJitBuilder(): OrcJitBuilder {
		return OrcJitBuilder()
	}

	fun createJit(builder: OrcJitBuilder = createJitBuilder()): OrcJit {
		val jit = OrcJit()
		val error = LLVMOrcCreateLLJIT(jit, builder)
		Llvm.handleError(error, "Failed to create JIT compiler")
		return jit
	}

	fun getMainLibrary(jit: OrcJit): OrcLibrary {
		return LLVMOrcLLJITGetMainJITDylib(jit)
	}

	fun createLibrary(executionSession: OrcExecutionSession, name: String): OrcLibrary {
		val library = OrcLibrary()
		val error = LLVMOrcExecutionSessionCreateJITDylib(executionSession, library, BytePointer(name))
		Llvm.handleError(error, "Failed to create library")
		return library
	}

	fun createTargetMachineBuilder(): OrcTargetMachineBuilder {
		return OrcTargetMachineBuilder()
	}

	fun detectHost(builder: OrcTargetMachineBuilder) {
		val error = LLVMOrcJITTargetMachineBuilderDetectHost(builder)
		Llvm.handleError(error, "Failed to detect host machine")
	}

	fun getTargetTriple(builder: OrcTargetMachineBuilder): String {
		val targetTriple = LLVMOrcJITTargetMachineBuilderGetTargetTriple(builder)
		return targetTriple.string
	}

	fun setTargetMachine(jit: OrcJitBuilder, targetMachine: OrcTargetMachineBuilder) {
		LLVMOrcLLJITBuilderSetJITTargetMachineBuilder(jit, targetMachine)
	}

	fun getExecutionSession(jit: OrcJit): OrcExecutionSession {
		return LLVMOrcLLJITGetExecutionSession(jit)
	}

	fun addModuleToLibrary(jit: OrcJit, library: OrcLibrary, threadSafeModule: OrcThreadSafeModule) {
		val error = LLVMOrcLLJITAddLLVMIRModule(jit, library, threadSafeModule)
		Llvm.handleError(error, "Failed to add module to JIT")
	}

	fun intern(jit: OrcJit, string: String): OrcString {
		return LLVMOrcLLJITMangleAndIntern(jit, string)
	}

	fun lookupInMainLibrary(jit: OrcJit, symbol: String): Pointer {
		val entrypoint = LongPointer(1)
		val error = LLVMOrcLLJITLookup(jit, entrypoint, symbol)
		Llvm.handleError(error, "Failed to find symbol '$symbol' in JIT main library")
		val addressAsLong = entrypoint.get()
		return Llvm.longToPointer(addressAsLong)
	}

	fun lookup(executionSession: OrcExecutionSession, library: OrcLibrary, symbol: OrcString): Pointer {
		val lookupFuture = CompletableFuture<Long>()
		val searchOrder = LLVMOrcCJITDylibSearchOrderElement()
		searchOrder.JD(library)
		searchOrder.JDLookupFlags(LLVMOrcJITDylibLookupFlagsMatchAllSymbols)
		val resultHandler = object: LLVMOrcExecutionSessionLookupHandleResultFunction() {

			override fun call(error: LlvmError?, result: LLVMOrcCSymbolMapPair?, count: Long, context: Pointer?) {
				try {
					Llvm.handleError(error, "Failed to resolve symbol in JIT")
					if(result == null)
						throw CompilerError("Symbol unexpectedly not present")
					lookupFuture.complete(result.Sym().Address())
				} catch(throwable: Throwable) {
					lookupFuture.completeExceptionally(throwable)
				}
			}
		}
		val symbols = LLVMOrcCLookupSetElement()
		symbols.Name(symbol)
		symbols.LookupFlags(LLVMOrcSymbolLookupFlagsRequiredSymbol)
		LLVMOrcExecutionSessionLookup(executionSession, LLVMOrcLookupKindStatic, searchOrder, 1, symbols, 1, resultHandler, null)
		val addressAsLong = lookupFuture.get()
		return Llvm.longToPointer(addressAsLong)
	}
}
