package components.code_generation.llvm

import org.bytedeco.llvm.global.LLVM.*

class LlvmDebugInfoConstructor(module: LlvmModule) {
	val builder = LlvmDebugInfo.createBuilder(module)

	companion object {
		const val producerName = "Pure compiler"
	}

	fun createCompileUnit(entrypointFile: LlvmDebugInfoMetadata): LlvmDebugInfoMetadata {
		val flags = ""
		val splitName = ""
		val sysRoot = ""
		val sdk = ""
		val runtimeVersion = 0
		return LLVMDIBuilderCreateCompileUnit(builder, LLVMDWARFSourceLanguageC, entrypointFile, producerName, producerName.length.toLong(),
			Llvm.NO, flags, flags.length.toLong(), runtimeVersion, splitName, splitName.length.toLong(), LLVMDWARFEmissionFull, 0,
			Llvm.NO, Llvm.NO, sysRoot, sysRoot.length.toLong(), sdk, sdk.length.toLong())
	}

	fun createFile(fileName: String, path: String): LlvmDebugInfoMetadata {
		return LLVMDIBuilderCreateFile(builder, fileName, fileName.length.toLong(), path, path.length.toLong())
	}

	fun finish() {
		LLVMDIBuilderFinalize(builder)
	}
}
