package components.code_generation.llvm.models.general

import components.code_generation.llvm.models.declarations.FunctionDefinition
import components.code_generation.llvm.models.declarations.ValueDeclaration
import components.code_generation.llvm.wrapper.LlvmBlock
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.general.Program
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Function
import errors.user.UserError
import util.ExitCode
import java.util.*

class Program(val context: Context, val model: Program, val files: List<File>) {

	companion object {
		const val GLOBAL_ENTRYPOINT_NAME = "main"
		const val RUNTIME_PREFIX = "pure_runtime_"
	}

	/**
	 * Compiles code to LLVM IR.
	 */
	fun compile(constructor: LlvmConstructor, userEntryPointPath: String? = null): LlvmValue {
		context.logger.addPhase("Compilation")
		context.externalFunctions.load(constructor)
		context.runtimeTypes.declare(constructor)
		context.runtimeStructs.declare(constructor)
		for(file in files)
			file.declare(constructor)
		for(file in files)
			file.define(constructor)
		context.standardLibrary.load(constructor, context, this)
		context.runtimeGlobals.declare(constructor, context)
		context.runtimeFunctions.build(constructor, context)
		context.nativeRegistry.loadNativeImplementations(constructor)
		for(file in files)
			file.compile(constructor)
		return createGlobalEntrypoint(constructor, getEntrypoint(userEntryPointPath))
	}

	private fun createGlobalEntrypoint(constructor: LlvmConstructor, userEntrypoint: UserEntrypoint?): LlvmValue {
		val userEntrypointFunction = userEntrypoint?.function
		val userEntryPointReturnsVoid =
			userEntrypointFunction == null || SpecialType.NOTHING.matches(userEntrypointFunction.model.signature.returnType)
		val globalEntryPointReturnType = if(userEntryPointReturnsVoid)
			constructor.i32Type
		else
			userEntrypointFunction.model.signature.returnType.getLlvmType(constructor)
		val globalEntryPointType =
			constructor.buildFunctionType(listOf(constructor.i32Type, constructor.pointerType), globalEntryPointReturnType)
		val globalEntryPoint = constructor.buildFunction(GLOBAL_ENTRYPOINT_NAME, globalEntryPointType)
		constructor.createAndSelectEntrypointBlock(globalEntryPoint)
		val uncaughtExceptionBlock = constructor.createBlock("uncaughtException")
		readProgramArguments(constructor)
		createStandardStreams(constructor)
		context.printDebugLine(constructor, "Initializing program...")
		val exceptionVariable = constructor.buildStackAllocation(constructor.pointerType, "__exceptionVariable", constructor.nullPointer)
		for(file in files) {
			constructor.buildFunctionCall(file.initializer, listOf(exceptionVariable))
			checkForUnhandledError(constructor, exceptionVariable, uncaughtExceptionBlock)
		}
		context.printDebugLine(constructor, "Program initialized.")
		context.printDebugLine(constructor, "Starting program...")
		var result: LlvmValue? = null
		if(userEntrypointFunction == null) {
			for(file in files) {
				constructor.buildFunctionCall(file.runner, listOf(exceptionVariable))
				checkForUnhandledError(constructor, exceptionVariable, uncaughtExceptionBlock)
			}
			context.printDebugLine(constructor, "Files executed.")
		} else {
			for(file in getFilesToInitialize(userEntrypoint.file)) {
				context.printDebugMessage("Initializing '${file.name}'")
				constructor.buildFunctionCall(file.runner, listOf(exceptionVariable))
				checkForUnhandledError(constructor, exceptionVariable, uncaughtExceptionBlock)
			}
			context.printDebugLine(constructor, "Files executed.")
			context.printDebugLine(constructor, "Calling entrypoint...")
			val parameters = LinkedList<LlvmValue>()
			parameters.add(exceptionVariable)
			val userEntrypointObject = userEntrypoint.objectDeclaration
			if(userEntrypointObject != null) {
				val objectAddress = constructor.buildLoad(userEntrypointObject.model.effectiveType?.getLlvmType(constructor),
					userEntrypointObject.llvmLocation, "objectAddress")
				parameters.add(objectAddress)
			}
			result = constructor.buildFunctionCall(userEntrypointFunction.model.signature.getLlvmType(constructor),
				userEntrypointFunction.llvmValue, parameters, if(userEntryPointReturnsVoid) "" else "programResult")
			checkForUnhandledError(constructor, exceptionVariable, uncaughtExceptionBlock)
			context.printDebugLine(constructor, "Entrypoint returned.")
		}

		if(userEntryPointReturnsVoid)
			result = constructor.buildInt32(ExitCode.SUCCESS)
		constructor.buildReturn(result)
		constructor.select(uncaughtExceptionBlock)
		reportUnhandledError(constructor, exceptionVariable)
		return globalEntryPoint
	}

	private fun getFilesToInitialize(userEntrypointFile: File): LinkedList<File> {
		val filesToInitialize = LinkedList<File>()
		userEntrypointFile.determineFileInitializationOrder(filesToInitialize)
		// Ensure that classes instantiated by the runtime are initialized
		if(context.nativeRegistry.has(SpecialType.EXCEPTION))
			context.standardLibrary.exceptionTypeDeclaration.determineFileInitializationOrder(filesToInitialize)
		else if(context.nativeRegistry.has(SpecialType.STRING))
			context.standardLibrary.stringTypeDeclaration.determineFileInitializationOrder(filesToInitialize)
		return filesToInitialize
	}

	private fun reportUnhandledError(constructor: LlvmConstructor, exceptionVariable: LlvmValue) {
		val exception = constructor.buildLoad(constructor.pointerType, exceptionVariable, "exception")
		if(context.nativeRegistry.has(SpecialType.EXCEPTION, SpecialType.STRING, SpecialType.ARRAY)) {
			val stringRepresentationGetterAddress = context.resolveFunction(constructor, exception, "get stringRepresentation: String")
			val ignoredExceptionVariable =
				constructor.buildStackAllocation(constructor.pointerType, "ignoredExceptionVariable", constructor.nullPointer)
			val stringRepresentation = constructor.buildFunctionCall(
				constructor.buildFunctionType(listOf(constructor.pointerType, constructor.pointerType), constructor.pointerType),
				stringRepresentationGetterAddress, listOf(ignoredExceptionVariable, exception), "stringRepresentation")
			val byteArrayProperty = context.resolveMember(constructor, stringRepresentation, "bytes")
			val byteArray = constructor.buildLoad(constructor.pointerType, byteArrayProperty, "bytes")
			val arraySizeProperty = context.resolveMember(constructor, byteArray, "size")
			val arraySize = constructor.buildLoad(constructor.i32Type, arraySizeProperty, "size")
			val byteArrayRuntimeClass = context.standardLibrary.byteArray
			val arrayValueProperty = byteArrayRuntimeClass.getNativeValueProperty(constructor, byteArray)
			val arrayValue = constructor.buildLoad(constructor.pointerType, arrayValueProperty, "arrayValue")
			context.printMessage(constructor, "Unhandled error: %.*s", arraySize, arrayValue)
			val exitCode = constructor.buildInt32(1)
			constructor.buildFunctionCall(context.externalFunctions.exit, listOf(exitCode))
		} else {
			context.panic(constructor, "Unhandled error at '%p'.", exception)
		}
		constructor.markAsUnreachable()
	}

	private fun checkForUnhandledError(constructor: LlvmConstructor, exceptionVariable: LlvmValue, uncaughtExceptionBlock: LlvmBlock) {
		val exception = constructor.buildLoad(constructor.pointerType, exceptionVariable, "exception")
		val doesExceptionExist = constructor.buildIsNotNull(exception, "doesExceptionExist")
		val noExceptionBlock = constructor.createBlock("noException")
		constructor.buildJump(doesExceptionExist, uncaughtExceptionBlock, noExceptionBlock)
		constructor.select(noExceptionBlock)
	}

	private fun readProgramArguments(constructor: LlvmConstructor) {
		constructor.buildStore(constructor.getParameter(0), context.runtimeGlobals.programArgumentCount)
		constructor.buildStore(constructor.getParameter(1), context.runtimeGlobals.programArgumentArray)
	}

	//TODO fix: this somehow corrupts the program (segfault)
	// - to test: make NativeAdder inherit from "Application" and run the in-memory test (on Windows)
	// - check how the zig std creates output streams
	private fun createStandardStreams(constructor: LlvmConstructor) {
		val targetTriple = constructor.getTargetTriple()
		if(targetTriple.contains("windows")) {
			//val functionType3 = constructor.buildFunctionType(listOf(), constructor.booleanType)
			//val function3 = constructor.buildFunction("__vcrt_initialize", functionType3)
			//constructor.buildFunctionCall(functionType3, function3)
			//val functionType = constructor.buildFunctionType(listOf(), constructor.booleanType)
			//val function = constructor.buildFunction("__acrt_initialize", functionType)
			//constructor.buildFunctionCall(functionType, function)
			//val functionType2 = constructor.buildFunctionType(listOf(), constructor.i32Type)
			//val function2 = constructor.buildFunction("__acrt_initialize_stdio", functionType2)
			//constructor.buildFunctionCall(functionType2, function2)
			//val functionType2 = constructor.buildFunctionType(listOf(), constructor.i32Type)
			//val function2 = constructor.buildFunction("_tmainCRTStartup", functionType2)
			//constructor.buildFunctionCall(functionType2, function2)
		}
		val inputStreamMode = constructor.buildGlobalAsciiCharArray("${RUNTIME_PREFIX}standard_input_stream_mode", "r")
		val outputStreamMode = constructor.buildGlobalAsciiCharArray("${RUNTIME_PREFIX}standard_output_stream_mode", "w")
		createStandardStream(constructor, 0, inputStreamMode, context.runtimeGlobals.standardInputStream)
		createStandardStream(constructor, 1, outputStreamMode, context.runtimeGlobals.standardOutputStream)
		createStandardStream(constructor, 2, outputStreamMode, context.runtimeGlobals.standardErrorStream)
	}

	private fun createStandardStream(constructor: LlvmConstructor, identifier: Int, mode: LlvmValue, global: LlvmValue) {
		//val handle =
		//	constructor.buildFunctionCall(context.externalFunctions.streamOpen, listOf(constructor.buildInt32(identifier)), "handle")
		val handle =
			constructor.buildFunctionCall(context.externalFunctions.streamOpen, listOf(constructor.buildInt32(identifier), mode), "handle")
		//val handle = constructor.buildGetArrayElementPointer(constructor.pointerType, handleArray, constructor.buildInt32(identifier), "handle")
		constructor.buildStore(handle, global)
	}

	fun getEntrypoint(entrypointPath: String?): UserEntrypoint? {
		if(entrypointPath == null) return null
		try {
			val pathSections = entrypointPath.split(":")
			if(pathSections.size != 2)
				throw UserError("Malformed entrypoint path '$entrypointPath'.")
			val (filePath, functionPath) = pathSections
			val file = getFile(filePath.split(".")) ?: throw UserError("File '$filePath' not found.")
			val functionPathParts = functionPath.split(".").toMutableList()
			val functionName = functionPathParts.removeLast()

			var scope: Scope = file.model.scope
			var objectDefinition: components.semantic_model.declarations.Object? = null
			for(objectName in functionPathParts) {
				objectDefinition =
					scope.getTypeDeclaration(objectName) as? components.semantic_model.declarations.Object ?: throw UserError(
						"Object '$objectName' not found.")
				if(objectDefinition.isBound)
					throw UserError("Object '$objectName' is bound.")
				scope = objectDefinition.scope
			}
			val functionVariable = scope.getValueDeclaration(functionName)?.declaration
				?: throw UserError("Function '$functionName' not found.")
			val function = functionVariable.value as? Function ?: throw UserError("Variable '$functionName' is not a function.")
			val functionImplementation = function.implementations.find { functionImplementation ->
				!functionImplementation.signature.requiresParameters()
			} ?: throw UserError("Function '$functionName' has no overload without parameters.")
			var objectValue: components.semantic_model.declarations.ValueDeclaration? = null
			if(objectDefinition != null)
				objectValue = (objectDefinition.parentTypeDeclaration?.scope ?: objectDefinition.scope)
					.getValueDeclaration(objectDefinition.name)?.declaration
			return UserEntrypoint(file, objectValue?.unit, functionImplementation.unit)
		} catch(error: UserError) {
			throw UserError("Failed to locate entrypoint: ${error.message}")
		}
	}

	fun getFile(pathParts: List<String>): File? {
		for(file in files)
			if(file.model.matches(pathParts))
				return file
		return null
	}

	class UserEntrypoint(val file: File, val objectDeclaration: ValueDeclaration?, val function: FunctionDefinition)
}
