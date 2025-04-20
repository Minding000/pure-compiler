package components.code_generation.llvm

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmFunction

class ExternalFunctions {
	/** Always adds null-termination */
	lateinit var print: LlvmFunction
	/** Always adds null-termination */
	lateinit var printToBuffer: LlvmFunction
	/** Always excludes null-termination */
	lateinit var printSize: LlvmFunction
	lateinit var parseDouble: LlvmFunction
	lateinit var streamOpen: LlvmFunction
	lateinit var streamError: LlvmFunction
	lateinit var streamClose: LlvmFunction
	lateinit var streamWrite: LlvmFunction
	lateinit var streamReadByte: LlvmFunction
	lateinit var streamRead: LlvmFunction
	lateinit var streamFlush: LlvmFunction
	lateinit var sleep: LlvmFunction
	lateinit var exit: LlvmFunction
	lateinit var memoryCopy: LlvmFunction
	lateinit var variableParameterIterationStart: LlvmFunction
	lateinit var variableParameterListCopy: LlvmFunction
	lateinit var variableParameterIterationEnd: LlvmFunction
	lateinit var si32Addition: LlvmFunction
	lateinit var si32Subtraction: LlvmFunction
	lateinit var si32Multiplication: LlvmFunction
	lateinit var si8Addition: LlvmFunction
	lateinit var si8Subtraction: LlvmFunction
	lateinit var si8Multiplication: LlvmFunction

	fun load(constructor: LlvmConstructor) {
		addPrintFunction(constructor)
		addPrintToBufferFunction(constructor)
		addPrintSizeFunction(constructor)
		addParseDoubleFunction(constructor)
		addStreamOpenFunction(constructor)
		addStreamErrorFunction(constructor)
		addStreamCloseFunction(constructor)
		addStreamWriteFunction(constructor)
		addStreamReadByteFunction(constructor)
		addStreamReadFunction(constructor)
		addStreamFlushFunction(constructor)
		addSleepFunction(constructor)
		addExitFunction(constructor)
		addMemoryCopyFunction(constructor)
		addVariadicIntrinsics(constructor)
		addMathematicalIntrinsics(constructor)
	}

	private fun addPrintFunction(constructor: LlvmConstructor) {
		print = LlvmFunction(constructor, "fprintf", listOf(constructor.pointerType, constructor.pointerType), constructor.i32Type, true)
	}

	private fun addPrintToBufferFunction(constructor: LlvmConstructor) {
		printToBuffer =
			LlvmFunction(constructor, "sprintf", listOf(constructor.pointerType, constructor.pointerType), constructor.i32Type, true)
	}

	private fun addPrintSizeFunction(constructor: LlvmConstructor) {
		val targetTriple = constructor.getTargetTriple()
		val name = if(targetTriple.contains("linux")) "snprintf" else "_snprintf"
		printSize = LlvmFunction(constructor, name, listOf(constructor.pointerType, constructor.i64Type, constructor.pointerType),
			constructor.i32Type, true)
	}

	private fun addParseDoubleFunction(constructor: LlvmConstructor) {
		parseDouble = LlvmFunction(constructor, "strtod", listOf(constructor.pointerType, constructor.pointerType), constructor.doubleType)
	}

	private fun addStreamOpenFunction(constructor: LlvmConstructor) {
		val targetTriple = constructor.getTargetTriple()
		//val name = if(targetTriple.contains("linux")) "fdopen" else "__acrt_iob_func"
		//streamOpen = LlvmFunction(constructor, name, listOf(constructor.i32Type), constructor.pointerType)

		//val name = if(targetTriple.contains("linux")) "fdopen" else "__iob_func"
		//streamOpen = LlvmFunction(constructor, name, listOf(), constructor.pointerType)
		val name = if(targetTriple.contains("windows")) "_fdopen" else "fdopen"
		streamOpen = LlvmFunction(constructor, name, listOf(constructor.i32Type, constructor.pointerType), constructor.pointerType)
	}

	private fun addStreamErrorFunction(constructor: LlvmConstructor) {
		streamError = LlvmFunction(constructor, "ferror", listOf(constructor.pointerType), constructor.i32Type)
	}

	private fun addStreamCloseFunction(constructor: LlvmConstructor) {
		streamClose = LlvmFunction(constructor, "fclose", listOf(constructor.pointerType), constructor.i32Type)
	}

	private fun addStreamWriteFunction(constructor: LlvmConstructor) {
		streamWrite = LlvmFunction(constructor, "fwrite",
			listOf(constructor.pointerType, constructor.i64Type, constructor.i64Type, constructor.pointerType), constructor.i64Type)
	}

	private fun addStreamReadByteFunction(constructor: LlvmConstructor) {
		streamReadByte = LlvmFunction(constructor, "fgetc", listOf(constructor.pointerType), constructor.i32Type)
	}

	private fun addStreamReadFunction(constructor: LlvmConstructor) {
		streamRead = LlvmFunction(constructor, "fread",
			listOf(constructor.pointerType, constructor.i64Type, constructor.i64Type, constructor.pointerType), constructor.i64Type)
	}

	private fun addStreamFlushFunction(constructor: LlvmConstructor) {
		streamFlush = LlvmFunction(constructor, "fflush", listOf(constructor.pointerType), constructor.i32Type)
	}

	private fun addSleepFunction(constructor: LlvmConstructor) {
		sleep = LlvmFunction(constructor, "Sleep", listOf(constructor.i32Type))
	}

	private fun addExitFunction(constructor: LlvmConstructor) {
		exit = LlvmFunction(constructor, "exit", listOf(constructor.i32Type))
	}

	private fun addMemoryCopyFunction(constructor: LlvmConstructor) {
		memoryCopy = LlvmFunction(constructor, "memcpy", listOf(constructor.pointerType, constructor.pointerType, constructor.i64Type),
			constructor.pointerType)
	}

	private fun addVariadicIntrinsics(constructor: LlvmConstructor) {
		variableParameterIterationStart = LlvmFunction(constructor, "llvm.va_start", listOf(constructor.pointerType))
		variableParameterListCopy = LlvmFunction(constructor, "llvm.va_copy", listOf(constructor.pointerType, constructor.pointerType))
		variableParameterIterationEnd = LlvmFunction(constructor, "llvm.va_end", listOf(constructor.pointerType))
	}

	private fun addMathematicalIntrinsics(constructor: LlvmConstructor) {
		val i32ReturnType = constructor.buildAggregateType(constructor.i32Type, constructor.booleanType)
		si32Addition =
			LlvmFunction(constructor, "llvm.sadd.with.overflow.i32", listOf(constructor.i32Type, constructor.i32Type), i32ReturnType)
		si32Subtraction =
			LlvmFunction(constructor, "llvm.ssub.with.overflow.i32", listOf(constructor.i32Type, constructor.i32Type), i32ReturnType)
		si32Multiplication =
			LlvmFunction(constructor, "llvm.smul.with.overflow.i32", listOf(constructor.i32Type, constructor.i32Type), i32ReturnType)
		val byteReturnType = constructor.buildAggregateType(constructor.byteType, constructor.booleanType)
		si8Addition =
			LlvmFunction(constructor, "llvm.sadd.with.overflow.i8", listOf(constructor.byteType, constructor.byteType), byteReturnType)
		si8Subtraction =
			LlvmFunction(constructor, "llvm.ssub.with.overflow.i8", listOf(constructor.byteType, constructor.byteType), byteReturnType)
		si8Multiplication =
			LlvmFunction(constructor, "llvm.smul.with.overflow.i8", listOf(constructor.byteType, constructor.byteType), byteReturnType)
	}
}
