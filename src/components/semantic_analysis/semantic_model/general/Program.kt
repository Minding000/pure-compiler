package components.semantic_analysis.semantic_model.general

import code.Main
import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.Context
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.Object
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Function
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
	fun compile(constructor: LlvmConstructor, entryPointPath: String? = null): LlvmValue {
		addPrintFunction(constructor)
		addExitFunction(constructor)
		context.llvmMemberIndexType = constructor.i32Type
		context.llvmMemberIdType = constructor.i32Type
		context.llvmMemberOffsetType = constructor.i32Type
		setUpSystemFunctions(constructor)
		for(file in files)
			file.declare(constructor)
		for(file in files)
			file.define(constructor)
		for(file in files)
			file.compile(constructor)
		val userEntryPoint = if(entryPointPath != null) getEntryPoint(entryPointPath) else null
		val entryPointType = userEntryPoint?.signature?.getLlvmType(constructor) ?: constructor.buildFunctionType()
		val globalEntryPoint = constructor.buildFunction("entrypoint", entryPointType)
		constructor.createAndSelectBlock(globalEntryPoint, "entrypoint")
		for(file in files)
			constructor.buildFunctionCall(file.llvmInitializerType, file.llvmInitializerValue)
		var result: LlvmValue? = null
		if(userEntryPoint != null) {
			val returnsVoid = SpecialType.NOTHING.matches(userEntryPoint.signature.returnType)
			result = constructor.buildFunctionCall(userEntryPoint.signature.getLlvmType(constructor), userEntryPoint.llvmValue, emptyList(), if(returnsVoid) "" else "programResult")
			if(returnsVoid)
				result = null
		}
		constructor.buildReturn(result)
		return globalEntryPoint
	}

	private fun addPrintFunction(constructor: LlvmConstructor) {
		val charArrayType = constructor.createPointerType(constructor.byteType)
		context.llvmPrintFunctionType = constructor.buildFunctionType(listOf(charArrayType), constructor.i32Type, true)
		context.llvmPrintFunction = constructor.buildFunction("printf", context.llvmPrintFunctionType)
	}

	private fun addExitFunction(constructor: LlvmConstructor) {
		context.llvmExitFunctionType = constructor.buildFunctionType(listOf(constructor.i32Type), constructor.voidType)
		context.llvmExitFunction = constructor.buildFunction("exit", context.llvmExitFunctionType)
	}

	private fun setUpSystemFunctions(constructor: LlvmConstructor) {
		// create class struct containing:
		// - mapping from signature to members
		// resolve member locations by signature and class
		// resolve function locations by signature and class
		// ignore performance and global function for now, KISS!

		// Question: How do function signatures look like?
		//  -> For now they map one-to-one to Pure signatures
		//  -> Later on intermediary signatures might get introduced
		// Question: What if only one of multiple function overloads is overridden?
		//  -> Just copy the other ones

		context.classDefinitionStruct = constructor.declareStruct("_ClassStruct")
		// The member count is not strictly required because the loop is guaranteed to find a matching member, but it is included to be safe.
		val memberCountType = constructor.i32Type
		val memberIdArrayType = constructor.createPointerType(context.llvmMemberIdType)
		val memberOffsetArrayType = constructor.createPointerType(context.llvmMemberOffsetType)
		constructor.defineStruct(context.classDefinitionStruct, listOf(memberCountType, memberIdArrayType, memberOffsetArrayType))
		context.functionStruct = constructor.declareStruct("_FunctionStruct")
		val implementationIdArrayType = constructor.createPointerType(context.llvmMemberIdType)
		val implementationOffsetArrayType = constructor.createPointerType(context.llvmMemberOffsetType)
		constructor.defineStruct(context.functionStruct, listOf(memberCountType, implementationIdArrayType, implementationOffsetArrayType))
		setUpMemberResolutionFunction(constructor)
		//setUpFunctionResolutionFunction(constructor)
	}

	private fun setUpMemberResolutionFunction(constructor: LlvmConstructor) {
		val classPointerType = constructor.createPointerType(context.classDefinitionStruct)
		context.llvmMemberOffsetFunctionType = constructor.buildFunctionType(listOf(classPointerType, context.llvmMemberIdType), context.llvmMemberOffsetType)
		context.llvmMemberOffsetFunction = constructor.buildFunction("_getMemberOffset", context.llvmMemberOffsetFunctionType)
		constructor.createAndSelectBlock(context.llvmMemberOffsetFunction, "entrypoint")
		val classDefinition = constructor.getParameter(context.llvmMemberOffsetFunction, 0)
		val targetMemberId = constructor.getParameter(context.llvmMemberOffsetFunction, 1)
		context.printDebugMessage(constructor, "Searching for member with ID '%i'.", targetMemberId)
		val memberCountAddress = constructor.buildGetPropertyPointer(context.classDefinitionStruct, classDefinition, Context.MEMBER_COUNT_PROPERTY_INDEX, "memberCountAddress")
		val memberCount = constructor.buildLoad(context.llvmMemberIndexType, memberCountAddress, "memberCount")
		val memberIdArrayLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, classDefinition, Context.MEMBER_ID_ARRAY_PROPERTY_INDEX, "memberIdArrayAddress")
		val memberIdArray = constructor.buildLoad(constructor.createPointerType(context.llvmMemberIdType), memberIdArrayLocation, "memberIdArray")
		val memberOffsetArrayLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, classDefinition, Context.MEMBER_OFFSET_ARRAY_PROPERTY_INDEX, "memberOffsetArrayAddress")
		val memberOffsetArray = constructor.buildLoad(constructor.createPointerType(context.llvmMemberOffsetType), memberOffsetArrayLocation, "memberOffsetArray")
		val indexVariableLocation = constructor.buildStackAllocation(constructor.i32Type, "indexLocation")
		constructor.buildStore(constructor.buildInt32(0), indexVariableLocation)
		val loopBlock = constructor.createBlock(context.llvmMemberOffsetFunction, "loop")
		constructor.buildJump(loopBlock)
		constructor.select(loopBlock)
		val currentIndex = constructor.buildLoad(context.llvmMemberIndexType, indexVariableLocation, "currentIndex")
		if(Main.DEBUG) {
			val outOfBounds = constructor.buildSignedIntegerEqualTo(currentIndex, memberCount, "boundsCheck")
			val panicBlock = constructor.createBlock(context.llvmMemberOffsetFunction, "panic")
			val idCheckBlock = constructor.createBlock(context.llvmMemberOffsetFunction, "idCheck")
			constructor.buildJump(outOfBounds, panicBlock, idCheckBlock)
			constructor.select(panicBlock)
			context.printDebugMessage(constructor, "Member with ID '%i' does not exist.", targetMemberId)
			val exitCode = constructor.buildInt32(1)
			constructor.buildFunctionCall(context.llvmExitFunctionType, context.llvmExitFunction, listOf(exitCode))
			constructor.markAsUnreachable()
			constructor.select(idCheckBlock)
		}
		val newIndex = constructor.buildIntegerAddition(currentIndex, constructor.buildInt32(1), "newIndex")
		constructor.buildStore(newIndex, indexVariableLocation)
		val currentIdLocation = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, memberIdArray, currentIndex, "currentIdLocation")
		val currentId = constructor.buildLoad(context.llvmMemberIdType, currentIdLocation, "currentId")
		val memberSearchHit = constructor.buildSignedIntegerEqualTo(currentId, targetMemberId, "idCheck")
		val returnBlock = constructor.createBlock(context.llvmMemberOffsetFunction, "exit")
		constructor.buildJump(memberSearchHit, returnBlock, loopBlock)
		constructor.select(returnBlock)
		val memberOffsetLocation = constructor.buildGetArrayElementPointer(context.llvmMemberOffsetType, memberOffsetArray, currentIndex, "memberOffsetLocation")
		val memberOffset = constructor.buildLoad(context.llvmMemberOffsetType, memberOffsetLocation, "memberOffset")
		constructor.buildReturn(memberOffset)
	}

	private fun setUpFunctionResolutionFunction(constructor: LlvmConstructor) {
		//TODO give each signature a unique ID (map parameters to ID in Context)
		val functionResolutionFunctionType = constructor.buildFunctionType(listOf(context.functionStruct, context.llvmMemberIdType), context.llvmMemberOffsetType)
		val functionResolutionFunction = constructor.buildFunction("_getFunctionOffset", functionResolutionFunctionType)
		constructor.createAndSelectBlock(functionResolutionFunction, "entrypoint")
		//TODO:
		// - loop over IDs
		// - if ID matches
		// - get address
		// - return address
	}

	fun getEntryPoint(entryPointPath: String): FunctionImplementation {
		val pathSections = entryPointPath.split(":")
		if(pathSections.size != 2)
			throw UserError("Malformed entry point path '$entryPointPath'.")
		val (filePath, functionPath) = pathSections
		val file = getFile(filePath.split(".")) ?: throw UserError("File '$filePath' not found.")
		val functionPathParts = functionPath.split(".").toMutableList()
		val functionName = functionPathParts.removeLast()
		var scope: Scope = file.scope
		for(objectName in functionPathParts) {
			val objectDefinition = scope.resolveType(objectName) as? Object ?: throw UserError("Object '$objectName' not found.")
			if(objectDefinition.isBound)
				throw UserError("Object '$objectName' is bound.")
			scope = objectDefinition.scope
		}
		val functionVariable = scope.resolveValue(functionName) ?: throw UserError("Function '$functionName' not found.")
		val function = functionVariable.value as? Function ?: throw UserError("Variable '$functionName' is not a function.")
		val functionImplementation = function.implementations.find { functionImplementation ->
			functionImplementation.signature.takesNoParameters() } ?: throw UserError("Function '$functionName' has no overload without parameters.")
		return functionImplementation
	}
}
