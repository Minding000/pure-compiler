package components.code_generation.llvm

import errors.internal.CompilerError
import org.bytedeco.llvm.global.LLVM.*
import util.toLlvmList

class LlvmDebugInfoConstructor(module: LlvmModule) {
	val builder = LlvmDebugInfo.createBuilder(module)
	val booleanType = LlvmDebugInfo.createBooleanType(builder)
	val byteType = LlvmDebugInfo.createByteType(builder)
	val i32Type = LlvmDebugInfo.create32BitIntegerType(builder)
	val floatType = LlvmDebugInfo.createFloatType(builder)

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

	fun createFunction(parent: LlvmDebugInfoMetadata, name: String, parentFile: LlvmDebugInfoMetadata,
					   functionType: LlvmDebugInfoMetadata): LlvmDebugInfoMetadata {
		val linkageName = ""
		val lineNumber = 0
		val lineNumberInScope = 0
		val flags = 0
		return LLVMDIBuilderCreateFunction(builder, parent, name, name.length.toLong(), linkageName, linkageName.length.toLong(),
			parentFile, lineNumber, functionType, Llvm.NO, Llvm.YES, lineNumberInScope, flags, Llvm.NO)
	}

	fun createFunctionType(parentFile: LlvmDebugInfoMetadata, parameters: List<LlvmDebugInfoMetadata?>): LlvmDebugInfoMetadata {
		val flags = 0
		return LLVMDIBuilderCreateSubroutineType(builder, parentFile, parameters.toLlvmList(), parameters.size, flags)
	}

	fun createTypeDeclaration(parent: LlvmDebugInfoMetadata, name: String, parentFile: LlvmDebugInfoMetadata,
							  sizeInBits: Long, alignmentInBits: Int, offsetInBits: Long): LlvmDebugInfoMetadata {
		val flags = 0
		val elements = emptyList<LlvmDebugInfoMetadata>()
		val vtable = null
		val uniqueIdentifier = ""
		val lineNumber = 0
		return LLVMDIBuilderCreateClassType(builder, parent, name, name.length.toLong(), parentFile, lineNumber, sizeInBits,
			alignmentInBits, offsetInBits, flags, null, elements.toLlvmList(), elements.size, vtable,
			null, uniqueIdentifier, uniqueIdentifier.length.toLong())
	}

	fun createType(typeDeclarationMetadata: LlvmDebugInfoMetadata?): LlvmDebugInfoMetadata {
		if(typeDeclarationMetadata == null)
			throw CompilerError("Missing type declaration in object type.")
		return LLVMDIBuilderCreateObjectPointerType(builder, typeDeclarationMetadata)
	}

	fun attach(metadata: LlvmDebugInfoMetadata, function: LlvmValue) {
		LLVMSetSubprogram(function, metadata)
	}

	fun finish() {
		LLVMDIBuilderFinalize(builder)
	}
}
