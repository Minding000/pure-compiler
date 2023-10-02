package components.semantic_model.general

import code.Main
import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.declarations.Object
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Function
import components.semantic_model.values.ValueDeclaration
import errors.internal.CompilerError
import errors.user.UserError
import java.util.*
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree

class Program(val context: Context, val source: ProgramSyntaxTree) {
	val files = LinkedList<File>()

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
	 * Compiles code to LLVM IR.
	 */
	fun compile(constructor: LlvmConstructor, userEntryPointPath: String? = null): LlvmValue {
		constructor.setTargetTriple("x86_64-pc-windows")
		addPrintFunction(constructor)
		addFlushFunction(constructor)
		addExitFunction(constructor)
		addVariadicIntrinsics(constructor)
		createClosureStruct(constructor)
		context.llvmMemberIndexType = constructor.i32Type
		context.llvmMemberIdType = constructor.i32Type
		context.llvmMemberOffsetType = constructor.i32Type
		context.llvmMemberAddressType = constructor.pointerType
		setUpSystemFunctions(constructor)
		for(file in files)
			file.declare(constructor)
		for(file in files)
			file.define(constructor)
		findArrayTypeDeclaration()
		findStringInitializer()
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
		val entryPointType = userEntryPointFunction?.signature?.getLlvmType(constructor) ?: constructor.buildFunctionType()
		val globalEntryPoint = constructor.buildFunction("entrypoint", entryPointType)
		constructor.createAndSelectBlock(globalEntryPoint, "entrypoint")
		context.printDebugMessage(constructor, "Starting program...")
		for(file in files)
			constructor.buildFunctionCall(file.llvmInitializerType, file.llvmInitializerValue)
		context.printDebugMessage(constructor, "File initializers completed.")
		var result: LlvmValue? = null
		if(userEntryPointFunction != null) {
			val returnsVoid = SpecialType.NOTHING.matches(userEntryPointFunction.signature.returnType)
			val parameters = LinkedList<LlvmValue>()
			val exceptionAddressLocation = constructor.buildStackAllocation(constructor.pointerType, "exceptionAddress")
			parameters.add(exceptionAddressLocation)
			if(userEntryPointObject != null) {
				val objectAddress = constructor.buildLoad(
					userEntryPointObject.type?.getLlvmType(constructor),
					userEntryPointObject.llvmLocation,
					"objectAddress"
				)
				parameters.add(objectAddress)
			}
			result = constructor.buildFunctionCall(
				userEntryPointFunction.signature.getLlvmType(constructor),
				userEntryPointFunction.llvmValue,
				parameters,
				if(returnsVoid) "" else "programResult"
			)
			//TODO check for uncaught exception (exceptionAddressLocation)
			if(returnsVoid)
				result = null
		}
		constructor.buildReturn(result)
		return globalEntryPoint
	}

	private fun addPrintFunction(constructor: LlvmConstructor) {
		context.llvmPrintFunctionType = constructor.buildFunctionType(listOf(constructor.pointerType), constructor.i32Type, true)
		context.llvmPrintFunction = constructor.buildFunction("printf", context.llvmPrintFunctionType)
	}

	private fun addFlushFunction(constructor: LlvmConstructor) {
		context.llvmFlushFunctionType = constructor.buildFunctionType(listOf(constructor.pointerType), constructor.i32Type)
		context.llvmFlushFunction = constructor.buildFunction("fflush", context.llvmPrintFunctionType)
	}

	private fun addExitFunction(constructor: LlvmConstructor) {
		context.llvmExitFunctionType = constructor.buildFunctionType(listOf(constructor.i32Type), constructor.voidType)
		context.llvmExitFunction = constructor.buildFunction("exit", context.llvmExitFunctionType)
	}

	private fun addVariadicIntrinsics(constructor: LlvmConstructor) {
		context.variadicParameterListStruct = constructor.declareStruct("_variadicParameterList")
		val targetTriple = constructor.getTargetTriple()
		val variadicParameterListStructMembers = if(targetTriple.contains("x86_64-unknown-linux"))
			listOf(constructor.i32Type, constructor.i32Type, constructor.pointerType, constructor.pointerType)
		else
			listOf(constructor.pointerType)
		constructor.defineStruct(context.variadicParameterListStruct, variadicParameterListStructMembers)
		context.llvmVariableParameterIterationStartFunctionType = constructor.buildFunctionType(listOf(constructor.pointerType), constructor.voidType)
		context.llvmVariableParameterIterationStartFunction = constructor.buildFunction("llvm.va_start", context.llvmVariableParameterIterationStartFunctionType)
		context.llvmVariableParameterListCopyFunctionType = constructor.buildFunctionType(listOf(constructor.pointerType, constructor.pointerType), constructor.voidType)
		context.llvmVariableParameterListCopyFunction = constructor.buildFunction("llvm.va_copy", context.llvmVariableParameterListCopyFunctionType)
		context.llvmVariableParameterIterationEndFunctionType = constructor.buildFunctionType(listOf(constructor.pointerType), constructor.voidType)
		context.llvmVariableParameterIterationEndFunction = constructor.buildFunction("llvm.va_end", context.llvmVariableParameterIterationEndFunctionType)
	}

	private fun createClosureStruct(constructor: LlvmConstructor) {
		context.closureStruct = constructor.declareStruct("_Closure")
		constructor.defineStruct(context.closureStruct, listOf(constructor.pointerType, constructor.i32Type, constructor.pointerType))
	}

	private fun setUpSystemFunctions(constructor: LlvmConstructor) {
		context.classDefinitionStruct = constructor.declareStruct("_ClassStruct")
		// The member count is not strictly required because the loop is guaranteed to find a matching member,
		//  but it is included for debugging and error reporting.
		val memberCountType = constructor.i32Type
		val memberIdArrayType = constructor.pointerType
		val memberOffsetArrayType = constructor.pointerType
		val memberAddressArrayType = constructor.pointerType
		val classDefinitionMemberTypes = listOf(
			memberCountType, memberIdArrayType, memberOffsetArrayType,
			memberCountType, memberIdArrayType, memberOffsetArrayType,
			memberCountType, memberIdArrayType, memberAddressArrayType
		)
		constructor.defineStruct(context.classDefinitionStruct, classDefinitionMemberTypes)
		setUpMemberResolutionFunction(constructor, "Constant")
		setUpMemberResolutionFunction(constructor, "Property")
		setUpFunctionResolutionFunction(constructor)
	}

	private fun setUpMemberResolutionFunction(constructor: LlvmConstructor, type: String) {
		val memberCountPropertyIndex: Int
		val memberIdArrayPropertyIndex: Int
		val memberOffsetArrayPropertyIndex: Int
		if(type == "Constant") {
			memberCountPropertyIndex = Context.CONSTANT_COUNT_PROPERTY_INDEX
			memberIdArrayPropertyIndex = Context.CONSTANT_ID_ARRAY_PROPERTY_INDEX
			memberOffsetArrayPropertyIndex = Context.CONSTANT_OFFSET_ARRAY_PROPERTY_INDEX
		} else {
			memberCountPropertyIndex = Context.PROPERTY_COUNT_PROPERTY_INDEX
			memberIdArrayPropertyIndex = Context.PROPERTY_ID_ARRAY_PROPERTY_INDEX
			memberOffsetArrayPropertyIndex = Context.PROPERTY_OFFSET_ARRAY_PROPERTY_INDEX
		}
		val classPointerType = constructor.pointerType
		val functionType = constructor.buildFunctionType(listOf(classPointerType, context.llvmMemberIdType), context.llvmMemberOffsetType)
		val function = constructor.buildFunction("_get${type}Offset", functionType)
		constructor.createAndSelectBlock(function, "entrypoint")
		val classDefinition = constructor.getParameter(function, 0)
		val targetMemberId = constructor.getParameter(function, 1)
		context.printDebugMessage(constructor, "Searching for member with ID '%i'.", targetMemberId)
		val memberCountAddress = constructor.buildGetPropertyPointer(context.classDefinitionStruct, classDefinition,
			memberCountPropertyIndex, "memberCountAddress")
		val memberCount = constructor.buildLoad(context.llvmMemberIndexType, memberCountAddress, "memberCount")
		val memberIdArrayLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, classDefinition,
			memberIdArrayPropertyIndex, "memberIdArrayAddress")
		val memberIdArray = constructor.buildLoad(constructor.pointerType, memberIdArrayLocation, "memberIdArray")
		val memberOffsetArrayLocation = constructor.buildGetPropertyPointer( context.classDefinitionStruct, classDefinition,
			memberOffsetArrayPropertyIndex, "memberOffsetArrayAddress")
		val memberOffsetArray = constructor.buildLoad(constructor.pointerType, memberOffsetArrayLocation, "memberOffsetArray")
		val indexVariableLocation = constructor.buildStackAllocation(constructor.i32Type, "indexLocation")
		constructor.buildStore(constructor.buildInt32(0), indexVariableLocation)
		val loopBlock = constructor.createBlock(function, "loop")
		constructor.buildJump(loopBlock)
		constructor.select(loopBlock)
		val currentIndex = constructor.buildLoad(context.llvmMemberIndexType, indexVariableLocation, "currentIndex")
		if(Main.DEBUG) {
			val outOfBounds = constructor.buildSignedIntegerEqualTo(currentIndex, memberCount, "boundsCheck")
			val panicBlock = constructor.createBlock(function, "panic")
			val idCheckBlock = constructor.createBlock(function, "idCheck")
			constructor.buildJump(outOfBounds, panicBlock, idCheckBlock)
			constructor.select(panicBlock)
			context.panic(constructor, "$type with ID '%i' does not exist.", targetMemberId)
			constructor.markAsUnreachable()
			constructor.select(idCheckBlock)
		}
		val newIndex = constructor.buildIntegerAddition(currentIndex, constructor.buildInt32(1), "newIndex")
		constructor.buildStore(newIndex, indexVariableLocation)
		val currentIdLocation = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, memberIdArray, currentIndex,
			"currentIdLocation")
		val currentId = constructor.buildLoad(context.llvmMemberIdType, currentIdLocation, "currentId")
		val memberSearchHit = constructor.buildSignedIntegerEqualTo(currentId, targetMemberId, "idCheck")
		val returnBlock = constructor.createBlock(function, "exit")
		constructor.buildJump(memberSearchHit, returnBlock, loopBlock)
		constructor.select(returnBlock)
		val memberOffsetLocation = constructor.buildGetArrayElementPointer(context.llvmMemberOffsetType, memberOffsetArray, currentIndex,
			"memberOffsetLocation")
		val memberOffset = constructor.buildLoad(context.llvmMemberOffsetType, memberOffsetLocation, "memberOffset")
		constructor.buildReturn(memberOffset)
		if(type == "Constant") {
			context.llvmConstantOffsetFunction = function
			context.llvmConstantOffsetFunctionType = functionType
		} else {
			context.llvmPropertyOffsetFunction = function
			context.llvmPropertyOffsetFunctionType = functionType
		}
	}

	private fun setUpFunctionResolutionFunction(constructor: LlvmConstructor) {
		val classPointerType = constructor.pointerType
		val functionType = constructor.buildFunctionType(listOf(classPointerType, context.llvmMemberIdType), context.llvmMemberAddressType)
		val function = constructor.buildFunction("_getFunctionAddress", functionType)
		constructor.createAndSelectBlock(function, "entrypoint")
		val classDefinition = constructor.getParameter(function, 0)
		val targetMemberId = constructor.getParameter(function, 1)
		context.printDebugMessage(constructor, "Searching for function with ID '%i'.", targetMemberId)
		val functionCountAddress = constructor.buildGetPropertyPointer(context.classDefinitionStruct, classDefinition,
			Context.FUNCTION_COUNT_PROPERTY_INDEX, "functionCountAddress")
		val functionCount = constructor.buildLoad(context.llvmMemberIndexType, functionCountAddress, "functionCount")
		val functionIdArrayLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, classDefinition,
			Context.FUNCTION_ID_ARRAY_PROPERTY_INDEX, "functionIdArrayAddress")
		val functionIdArray = constructor.buildLoad(constructor.pointerType, functionIdArrayLocation, "functionIdArray")
		val functionAddressArrayLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, classDefinition,
			Context.FUNCTION_ADDRESS_ARRAY_PROPERTY_INDEX, "functionAddressArrayAddress")
		val functionAddressArray = constructor.buildLoad(constructor.pointerType, functionAddressArrayLocation,
			"functionAddressArray")
		val indexVariableLocation = constructor.buildStackAllocation(constructor.i32Type, "indexLocation")
		constructor.buildStore(constructor.buildInt32(0), indexVariableLocation)
		val loopBlock = constructor.createBlock(function, "loop")
		constructor.buildJump(loopBlock)
		constructor.select(loopBlock)
		val currentIndex = constructor.buildLoad(context.llvmMemberIndexType, indexVariableLocation, "currentIndex")
		if(Main.DEBUG) {
			val outOfBounds = constructor.buildSignedIntegerEqualTo(currentIndex, functionCount, "boundsCheck")
			val panicBlock = constructor.createBlock(function, "panic")
			val idCheckBlock = constructor.createBlock(function, "idCheck")
			constructor.buildJump(outOfBounds, panicBlock, idCheckBlock)
			constructor.select(panicBlock)
			context.panic(constructor, "Function with ID '%i' does not exist.", targetMemberId)
			constructor.markAsUnreachable()
			constructor.select(idCheckBlock)
		}
		val newIndex = constructor.buildIntegerAddition(currentIndex, constructor.buildInt32(1), "newIndex")
		constructor.buildStore(newIndex, indexVariableLocation)
		val currentIdLocation = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, functionIdArray, currentIndex,
			"currentIdLocation")
		val currentId = constructor.buildLoad(context.llvmMemberIdType, currentIdLocation, "currentId")
		val memberSearchHit = constructor.buildSignedIntegerEqualTo(currentId, targetMemberId, "idCheck")
		val returnBlock = constructor.createBlock(function, "exit")
		constructor.buildJump(memberSearchHit, returnBlock, loopBlock)
		constructor.select(returnBlock)
		val functionAddressLocation = constructor.buildGetArrayElementPointer(context.llvmMemberAddressType,functionAddressArray,
			currentIndex, "functionAddressLocation")
		val functionAddress = constructor.buildLoad(context.llvmMemberAddressType, functionAddressLocation, "functionAddress")
		constructor.buildReturn(functionAddress)
		context.llvmFunctionAddressFunction = function
		context.llvmFunctionAddressFunctionType = functionType
	}

	private fun findArrayTypeDeclaration() {
		val fileScope = SpecialType.ARRAY.fileScope
		context.arrayTypeDeclaration = fileScope?.getTypeDeclaration(SpecialType.ARRAY.className)
	}

	private fun findStringInitializer() {
		val fileScope = SpecialType.STRING.fileScope
		val typeDeclaration = fileScope?.getTypeDeclaration(SpecialType.STRING.className)
		context.stringTypeDeclaration = typeDeclaration
		if(typeDeclaration == null)
			return
		val byteArrayInitializer = typeDeclaration.getAllInitializers().find { initializerDefinition ->
			initializerDefinition.parameters.size == 1 }
			?: throw CompilerError(typeDeclaration.source, "Failed to find string literal initializer.")
		context.llvmStringByteArrayInitializer = byteArrayInitializer.llvmValue
		context.llvmStringByteArrayInitializerType = byteArrayInitializer.llvmType
	}

	fun getEntryPoint(entryPointPath: String): Pair<ValueDeclaration?, FunctionImplementation> {
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
		val (functionVariable) = scope.getValueDeclaration(functionName)
		if(functionVariable == null)
			throw UserError("Function '$functionName' not found.")
		val function = functionVariable.value as? Function ?: throw UserError("Variable '$functionName' is not a function.")
		val functionImplementation = function.implementations.find { functionImplementation ->
			!functionImplementation.signature.requiresParameters() }
			?: throw UserError("Function '$functionName' has no overload without parameters.")
		var objectValue: ValueDeclaration? = null
		if(objectDefinition != null) {
			val (objectDeclaration) = (objectDefinition.parentTypeDeclaration?.scope ?: objectDefinition.scope).getValueDeclaration(objectDefinition.name)
			objectValue = objectDeclaration
		}
		return Pair(objectValue, functionImplementation)
	}
}
