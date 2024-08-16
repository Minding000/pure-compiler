package components.code_generation.llvm.wrapper

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*

// LLVM
typealias LlvmContext = LLVMContextRef
typealias LlvmModule = LLVMModuleRef
typealias LlvmBuilder = LLVMBuilderRef
typealias LlvmBlock = LLVMBasicBlockRef
typealias LlvmValue = LLVMValueRef
typealias LlvmGenericValue = LLVMGenericValueRef
typealias LlvmType = LLVMTypeRef
typealias LlvmExecutionEngine = LLVMExecutionEngineRef
typealias LlvmList<T> = PointerPointer<T>

// LLVM debug info
typealias LlvmDebugInfoBuilder = LLVMDIBuilderRef
typealias LlvmDebugInfoMetadata = LLVMMetadataRef

// LLVM ORC
typealias OrcThreadSafeModule = LLVMOrcThreadSafeModuleRef
typealias OrcExecutionSession = LLVMOrcExecutionSessionRef
typealias OrcJit = LLVMOrcLLJITRef
typealias OrcJitBuilder = LLVMOrcLLJITBuilderRef
typealias OrcTargetMachineBuilder = LLVMOrcJITTargetMachineBuilderRef
typealias OrcString = LLVMOrcSymbolStringPoolEntryRef
typealias OrcLibrary = LLVMOrcJITDylibRef
typealias LlvmError = LLVMErrorRef
