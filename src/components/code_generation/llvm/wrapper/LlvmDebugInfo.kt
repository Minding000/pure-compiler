package components.code_generation.llvm.wrapper

import org.bytedeco.llvm.global.LLVM

object LlvmDebugInfo {

	fun createBuilder(module: LlvmModule): LlvmDebugInfoBuilder {
		return LLVM.LLVMCreateDIBuilder(module)
	}

	fun createBooleanType(builder: LlvmDebugInfoBuilder): LlvmDebugInfoMetadata {
		val name = "Boolean"
		val sizeInBits = 1L
		return LLVM.LLVMDIBuilderCreateBasicType(builder, name, name.length.toLong(), sizeInBits, 0, 0)
	}

	fun createByteType(builder: LlvmDebugInfoBuilder): LlvmDebugInfoMetadata {
		val name = "Byte"
		val sizeInBits = 8L
		return LLVM.LLVMDIBuilderCreateBasicType(builder, name, name.length.toLong(), sizeInBits, 0, 0)
	}

	fun create32BitIntegerType(builder: LlvmDebugInfoBuilder): LlvmDebugInfoMetadata {
		val name = "Int"
		val sizeInBits = 32L
		return LLVM.LLVMDIBuilderCreateBasicType(builder, name, name.length.toLong(), sizeInBits, 0, 0)
	}

	fun createFloatType(builder: LlvmDebugInfoBuilder): LlvmDebugInfoMetadata {
		val name = "Float"
		val sizeInBits = 32L
		return LLVM.LLVMDIBuilderCreateBasicType(builder, name, name.length.toLong(), sizeInBits, 0, 0)
	}
}
