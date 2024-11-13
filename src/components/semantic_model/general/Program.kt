package components.semantic_model.general

import components.code_generation.llvm.models.general.Program
import components.code_generation.llvm.wrapper.LlvmBlock
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.declarations.Object
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Function
import errors.user.UserError
import util.ExitCode
import java.util.*
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree

class Program(val context: Context, val source: ProgramSyntaxTree) {
	val files = LinkedList<File>()

	companion object {
		const val GLOBAL_ENTRYPOINT_NAME = "main"
		const val RUNTIME_PREFIX = "pure_runtime_"
	}

	fun getFile(pathParts: List<String>): File? {
		for(file in files)
			if(file.matches(pathParts))
				return file
		return null
	}

	/**
	 * Declares types and values.
	 */
	fun declare() {
		for(file in files)
			file.declare()
	}

	/**
	 * Resolves file references by listing files providing types.
	 */
	fun resolveFileReferences() {
		for(file in files)
			file.resolveFileReferences(this)
	}

	/**
	 * Determines the type of values.
	 */
	fun determineTypes() {
		for(file in files)
			file.determineTypes()
	}

	/**
	 * Collects information about variable usage order.
	 */
	fun analyseDataFlow() {
		for(file in files)
			file.analyseDataFlow()
	}

	/**
	 * Validates various rules including type- and null-safety.
	 */
	fun validate() {
		for(file in files)
			file.validate()
	}

	/**
	 * Creates a LLVM representation of the program.
	 */
	fun toUnit(): Program {
		return Program(context, this, files.map(File::toUnit))
	}

	/**
	 * Compiles code to LLVM IR.
	 */
	fun compile(constructor: LlvmConstructor, userEntryPointPath: String? = null): LlvmValue { //TODO remove
		context.logger.addPhase("Compilation")
		context.externalFunctions.load(constructor)
		context.runtimeTypes.declare(constructor)
		context.runtimeStructs.declare(constructor)
		for(file in files)
			file.declare(constructor)
		for(file in files)
			file.define(constructor)
		//context.standardLibrary.load(constructor, context)
		context.runtimeGlobals.declare(constructor, context)
		context.runtimeFunctions.build(constructor, context)
		context.nativeRegistry.loadNativeImplementations(constructor)
		for(file in files)
			file.compile(constructor)
		var userEntryPointObject: ValueDeclaration? = null
		var userEntryPointFunction: FunctionImplementation? = null
		if(userEntryPointPath != null) {
			val entryPointData = getEntryPoint(userEntryPointPath)
			userEntryPointObject = entryPointData.first
			userEntryPointFunction = entryPointData.second
		}
		return createGlobalEntrypoint(constructor, userEntryPointObject, userEntryPointFunction)
	}

	private fun createGlobalEntrypoint(constructor: LlvmConstructor, userEntryPointObject: ValueDeclaration?,
									   userEntryPointFunction: FunctionImplementation?): LlvmValue {
		val userEntryPointReturnsVoid = SpecialType.NOTHING.matches(userEntryPointFunction?.signature?.returnType)
		val globalEntryPointReturnType = if(userEntryPointReturnsVoid)
			constructor.i32Type
		else
			userEntryPointFunction?.signature?.returnType?.getLlvmType(constructor) ?: constructor.voidType
		val globalEntryPointType = constructor.buildFunctionType(emptyList(), globalEntryPointReturnType)
		val globalEntryPoint = constructor.buildFunction(GLOBAL_ENTRYPOINT_NAME, globalEntryPointType)
		constructor.createAndSelectEntrypointBlock(globalEntryPoint)
		val uncaughtExceptionBlock = constructor.createBlock("uncaughtException")
		createStandardStreams(constructor)
		context.printDebugLine(constructor, "Initializing program...")
		val exceptionVariable = constructor.buildStackAllocation(constructor.pointerType, "__exceptionVariable")
		constructor.buildStore(constructor.nullPointer, exceptionVariable)
		for(file in files) {
			constructor.buildFunctionCall(file.initializer, listOf(exceptionVariable))
			checkForUnhandledError(constructor, exceptionVariable, uncaughtExceptionBlock)
		}
		context.printDebugLine(constructor, "Program initialized.")
		context.printDebugLine(constructor, "Starting program...")
		var result: LlvmValue? = null
		if(userEntryPointFunction == null) {
			for(file in files) {
				constructor.buildFunctionCall(file.runner, listOf(exceptionVariable))
				checkForUnhandledError(constructor, exceptionVariable, uncaughtExceptionBlock)
			}
			context.printDebugLine(constructor, "Files executed.")
		} else {
			for(file in getFilesToInitialize(userEntryPointFunction).reversed()) {
				println("Initializing '${file.file.name}'")
				constructor.buildFunctionCall(file.runner, listOf(exceptionVariable))
				checkForUnhandledError(constructor, exceptionVariable, uncaughtExceptionBlock)
			}
			context.printDebugLine(constructor, "Files executed.")
			context.printDebugLine(constructor, "Calling entrypoint...")
			val parameters = LinkedList<LlvmValue>()
			parameters.add(exceptionVariable)
			if(userEntryPointObject != null) {
				val objectAddress = constructor.buildLoad(userEntryPointObject.effectiveType?.getLlvmType(constructor),
					userEntryPointObject.llvmLocation, "objectAddress")
				parameters.add(objectAddress)
			}
			result = constructor.buildFunctionCall(userEntryPointFunction.signature.getLlvmType(constructor),
				userEntryPointFunction.llvmValue, parameters, if(userEntryPointReturnsVoid) "" else "programResult")
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

	private fun getFilesToInitialize(userEntryPointFunction: FunctionImplementation): LinkedHashSet<File> {
		val filesToInitialize = LinkedHashSet<File>()
		userEntryPointFunction.getSurrounding<File>()?.determineFileInitializationOrder(filesToInitialize)
		// Ensure that classes instantiated by the runtime are initialized
		if(context.nativeRegistry.has(SpecialType.EXCEPTION))
			context.standardLibrary.exceptionTypeDeclaration.model.determineFileInitializationOrder(filesToInitialize)
		else if(context.nativeRegistry.has(SpecialType.STRING))
			context.standardLibrary.stringTypeDeclaration.model.determineFileInitializationOrder(filesToInitialize)
		return filesToInitialize
	}

	private fun reportUnhandledError(constructor: LlvmConstructor, exceptionVariable: LlvmValue) {
		val exception = constructor.buildLoad(constructor.pointerType, exceptionVariable, "exception")
		if(context.nativeRegistry.has(SpecialType.EXCEPTION, SpecialType.STRING, SpecialType.ARRAY)) {
			val stringRepresentationGetterAddress = context.resolveFunction(constructor, exception, "get stringRepresentation: String")
			val ignoredExceptionVariable = constructor.buildStackAllocation(constructor.pointerType, "ignoredExceptionVariable")
			constructor.buildStore(constructor.nullPointer, ignoredExceptionVariable)
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
		val handle =
			constructor.buildFunctionCall(context.externalFunctions.streamOpen, listOf(constructor.buildInt32(identifier)), "handle")
		//val handle = constructor.buildGetArrayElementPointer(constructor.pointerType, handleArray, constructor.buildInt32(identifier), "handle")
		constructor.buildStore(handle, global)
	}

	fun getEntryPoint(entryPointPath: String): Pair<ValueDeclaration?, FunctionImplementation> {
		try {
			val pathSections = entryPointPath.split(":")
			if(pathSections.size != 2)
				throw UserError("Malformed entry point path '$entryPointPath'.")
			val (filePath, functionPath) = pathSections
			val file = getFile(filePath.split(".")) ?: throw UserError("File '$filePath' not found.")
			val functionPathParts = functionPath.split(".").toMutableList()
			val functionName = functionPathParts.removeLast()
			var scope: Scope = file.scope
			var objectDefinition: Object? = null
			for(objectName in functionPathParts) {
				objectDefinition = scope.getTypeDeclaration(objectName) as? Object ?: throw UserError("Object '$objectName' not found.")
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
			var objectValue: ValueDeclaration? = null
			if(objectDefinition != null)
				objectValue = (objectDefinition.parentTypeDeclaration?.scope ?: objectDefinition.scope)
					.getValueDeclaration(objectDefinition.name)?.declaration
			return Pair(objectValue, functionImplementation)
		} catch(error: UserError) {
			throw UserError("Failed to locate entrypoint: ${error.message}")
		}
	}
}
