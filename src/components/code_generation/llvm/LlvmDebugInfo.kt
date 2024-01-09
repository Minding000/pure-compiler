package components.code_generation.llvm

import org.bytedeco.llvm.LLVM.LLVMDIBuilderRef
import org.bytedeco.llvm.LLVM.LLVMMetadataRef
import org.bytedeco.llvm.global.LLVM.LLVMCreateDIBuilder
import org.bytedeco.llvm.global.LLVM.LLVMDIBuilderCreateBasicType

object LlvmDebugInfo {

	fun createBuilder(module: LlvmModule): LlvmDebugInfoBuilder {
		return LLVMCreateDIBuilder(module)
	}

	fun createBooleanType(builder: LlvmDebugInfoBuilder): LlvmDebugInfoMetadata {
		val name = "Boolean"
		val sizeInBits = 1L
		return LLVMDIBuilderCreateBasicType(builder, name, name.length.toLong(), sizeInBits, 0, 0)
	}

	fun createByteType(builder: LlvmDebugInfoBuilder): LlvmDebugInfoMetadata {
		val name = "Byte"
		val sizeInBits = 8L
		return LLVMDIBuilderCreateBasicType(builder, name, name.length.toLong(), sizeInBits, 0, 0)
	}

	fun create32BitIntegerType(builder: LlvmDebugInfoBuilder): LlvmDebugInfoMetadata {
		val name = "Int"
		val sizeInBits = 32L
		return LLVMDIBuilderCreateBasicType(builder, name, name.length.toLong(), sizeInBits, 0, 0)
	}

	fun createFloatType(builder: LlvmDebugInfoBuilder): LlvmDebugInfoMetadata {
		val name = "Float"
		val sizeInBits = 32L
		return LLVMDIBuilderCreateBasicType(builder, name, name.length.toLong(), sizeInBits, 0, 0)
	}
}

typealias LlvmDebugInfoBuilder = LLVMDIBuilderRef
typealias LlvmDebugInfoMetadata = LLVMMetadataRef
