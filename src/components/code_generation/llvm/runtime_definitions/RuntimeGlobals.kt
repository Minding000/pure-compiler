package components.code_generation.llvm.runtime_definitions

import code.Main
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.general.Program.Companion.RUNTIME_PREFIX

class RuntimeGlobals {
	lateinit var symbolTable: LlvmValue
	/** Not available on Windows. See ExternalFunctions.windowsGetEnvironmentStrings */
	lateinit var environmentStringArray: LlvmValue
	lateinit var programArgumentCount: LlvmValue
	lateinit var programArgumentArray: LlvmValue
	lateinit var standardInputStream: LlvmValue
	lateinit var standardOutputStream: LlvmValue
	lateinit var standardErrorStream: LlvmValue

	fun declare(constructor: LlvmConstructor, context: Context) {
		if(Main.shouldPrintRuntimeDebugOutput)
			createSymbolTable(constructor, context)
		declareProgramInputs(constructor)
		declareStandardStreams(constructor)
	}

	private fun createSymbolTable(constructor: LlvmConstructor, context: Context) {
		val symbolTableSize = context.memberIdentities.ids.size + 1
		val symbols = Array<LlvmValue>(symbolTableSize) { constructor.nullPointer }
		for((symbol, index) in context.memberIdentities.ids)
			symbols[index] = constructor.buildGlobalAsciiCharArray("${RUNTIME_PREFIX}symbol", symbol)
		val symbolTableType = constructor.buildArrayType(constructor.pointerType, symbolTableSize)
		symbolTable = constructor.declareGlobal("${RUNTIME_PREFIX}symbolTable", symbolTableType)
		constructor.defineGlobal(symbolTable, constructor.buildConstantPointerArray(symbols.toList()))
	}

	private fun declareProgramInputs(constructor: LlvmConstructor) {
		val targetTriple = constructor.getTargetTriple()
		if(!targetTriple.contains("windows"))
			environmentStringArray = constructor.declareGlobal("environ", constructor.pointerType)
		programArgumentCount = constructor.declareGlobal("${RUNTIME_PREFIX}argument_count", constructor.i32Type)
		programArgumentArray = constructor.declareGlobal("${RUNTIME_PREFIX}argument_array", constructor.pointerType)
		constructor.defineGlobal(programArgumentCount, constructor.buildInt32(0))
		constructor.defineGlobal(programArgumentArray, constructor.nullPointer)
	}

	private fun declareStandardStreams(constructor: LlvmConstructor) {
		standardInputStream = constructor.declareGlobal("${RUNTIME_PREFIX}standard_input_stream", constructor.pointerType)
		standardOutputStream = constructor.declareGlobal("${RUNTIME_PREFIX}standard_output_stream", constructor.pointerType)
		standardErrorStream = constructor.declareGlobal("${RUNTIME_PREFIX}standard_error_stream", constructor.pointerType)
		constructor.defineGlobal(standardInputStream, constructor.nullPointer)
		constructor.defineGlobal(standardOutputStream, constructor.nullPointer)
		constructor.defineGlobal(standardErrorStream, constructor.nullPointer)
	}
}
