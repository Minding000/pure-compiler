package components.semantic_analysis.semantic_model.general

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
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
	private lateinit var llvmIndexType: LlvmType
	private lateinit var llvmIdType: LlvmType
	private lateinit var llvmAddressType: LlvmType

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
		llvmIndexType = constructor.i32Type
		llvmIdType = constructor.i32Type
		llvmAddressType = constructor.createPointerType(constructor.voidType)
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

	private fun setUpSystemFunctions(constructor: LlvmConstructor) { //TODO setup class structs
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

		context.classStruct = constructor.declareStruct("ClassStruct")
		val memberIdArrayType = constructor.createPointerType(llvmIdType)
		val memberAddressArrayType = constructor.createPointerType(llvmAddressType)
		constructor.defineStruct(context.classStruct, listOf(memberIdArrayType, memberAddressArrayType))
		context.functionStruct = constructor.declareStruct("FunctionStruct")
		val implementationIdArrayType = constructor.createPointerType(llvmIdType)
		val implementationAddressArrayType = constructor.createPointerType(llvmAddressType)
		constructor.defineStruct(context.functionStruct, listOf(implementationIdArrayType, implementationAddressArrayType))
		setUpMemberResolutionFunction(constructor)
		//setUpFunctionResolutionFunction(constructor)
	}

	private fun setUpMemberResolutionFunction(constructor: LlvmConstructor) {
		val classPointerType = constructor.createPointerType(context.classStruct)
		val memberResolutionFunctionType = constructor.buildFunctionType(listOf(classPointerType, llvmIdType), llvmAddressType)
		val memberResolutionFunction = constructor.buildFunction("resolveMember", memberResolutionFunctionType)
		constructor.createAndSelectBlock(memberResolutionFunction, "function_entry")
		// get class parameter
		val classObject = constructor.getParameter(memberResolutionFunction, 0)
		// get ID parameter
		val targetMemberId = constructor.getParameter(memberResolutionFunction, 1)
		// get member ID array location
		val memberIdArrayLocation = constructor.buildGetPropertyPointer(context.classStruct, classObject, 0, "memberIdArray")
		// get member location array location
		val memberLocationArrayLocation = constructor.buildGetPropertyPointer(context.classStruct, classObject, 1, "memberLocationArray")
		// declare index variable
		val indexVariableLocation = constructor.buildAllocation(constructor.i32Type, "memberIndex")
		// store 0 in index
		constructor.buildStore(constructor.buildInt32(0), indexVariableLocation)
		// create loop block
		val loopBlock = constructor.createBlock(memberResolutionFunction, "loop")
		constructor.buildJump(loopBlock)
		constructor.select(loopBlock)
		// get current index
		val currentIndexVariable = constructor.buildLoad(llvmIndexType, indexVariableLocation, "indexVariable")
		// get current ID location
		val currentIdVariableLocation = constructor.buildGetArrayElementPointer(memberIdArrayLocation, currentIndexVariable, "currentIndexVariable")
		// load current ID
		val currentIdVariable = constructor.buildLoad(llvmIdType, currentIdVariableLocation, "currentId")
		// compare ID
		val memberSearchHit = constructor.buildSignedIntegerEqualTo(currentIdVariable, targetMemberId, "idCheck")
		// jump based on result
		// - if same jump to loop end
		// - else jump to loop start
		val returnBlock = constructor.createBlock(memberResolutionFunction, "return")
		constructor.buildJump(memberSearchHit, returnBlock, loopBlock)
		constructor.select(returnBlock)
		// get member address location
		val currentMemberAddressVariableLocation = constructor.buildGetArrayElementPointer(memberLocationArrayLocation, currentIndexVariable, "currentAddressVariable")
		// load member address
		val currentMemberAddressVariable = constructor.buildLoad(llvmAddressType, currentMemberAddressVariableLocation, "currentMemberAddress")
		// return address
		constructor.buildReturn(currentMemberAddressVariable)
	}

	private fun setUpFunctionResolutionFunction(constructor: LlvmConstructor) {
		//TODO give each signature a unique ID (map parameters to ID in Context)
		val functionResolutionFunctionType = constructor.buildFunctionType(listOf(context.functionStruct, llvmIdType), llvmAddressType)
		val functionResolutionFunction = constructor.buildFunction("resolveFunction", functionResolutionFunctionType)
		constructor.createAndSelectBlock(functionResolutionFunction, "function_entry")
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
