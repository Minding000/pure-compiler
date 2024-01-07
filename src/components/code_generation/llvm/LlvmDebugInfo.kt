package components.code_generation.llvm

import org.bytedeco.llvm.LLVM.LLVMDIBuilderRef
import org.bytedeco.llvm.LLVM.LLVMMetadataRef
import org.bytedeco.llvm.global.LLVM.LLVMCreateDIBuilder

object LlvmDebugInfo {

	fun createBuilder(module: LlvmModule): LlvmDebugInfoBuilder {
		return LLVMCreateDIBuilder(module)
	}
}

typealias LlvmDebugInfoBuilder = LLVMDIBuilderRef
typealias LlvmDebugInfoMetadata = LLVMMetadataRef
