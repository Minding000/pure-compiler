package components.code_generation.llvm.runtime_definitions

import code.Main
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmFunction
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.general.Program.Companion.RUNTIME_PREFIX
import errors.internal.CompilerError

class RuntimeFunctions {
	lateinit var constantOffsetResolution: LlvmFunction
	lateinit var propertyOffsetResolution: LlvmFunction
	lateinit var functionAddressResolution: LlvmFunction
	lateinit var createString: LlvmFunction
	lateinit var addExceptionLocation: LlvmFunction

	fun build(constructor: LlvmConstructor, context: Context) {
		buildMemberResolutionFunction(constructor, context, "Constant")
		buildMemberResolutionFunction(constructor, context, "Property")
		buildFunctionResolutionFunction(constructor, context)
		if(context.nativeRegistry.has(SpecialType.STRING))
			buildCreateStringFunction(constructor, context)
		if(context.nativeRegistry.has(SpecialType.EXCEPTION))
			buildAddExceptionLocationFunction(constructor, context)
	}

	private fun buildMemberResolutionFunction(constructor: LlvmConstructor, context: Context, type: String) {
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
		val functionType =
			constructor.buildFunctionType(listOf(classPointerType, context.runtimeTypes.memberId), context.runtimeTypes.memberOffset)
		val function = constructor.buildFunction("${RUNTIME_PREFIX}get${type}Offset", functionType)
		constructor.createAndSelectEntrypointBlock(function)
		val classDefinition = constructor.getParameter(function, 0)
		val targetMemberId = constructor.getParameter(function, 1)
		if(Main.shouldPrintRuntimeDebugOutput) {
			val targetMemberIdentifier = context.resolveMemberIdentifier(constructor, targetMemberId)
			context.printDebugLine(constructor, "Searching for ${type.lowercase()} '%s' in class definition at '%p'.",
				targetMemberIdentifier, classDefinition)
		}
		val memberCountProperty = constructor.buildGetPropertyPointer(context.runtimeStructs.classDefinition, classDefinition,
			memberCountPropertyIndex, "memberCountProperty")
		val memberCount = constructor.buildLoad(context.runtimeTypes.memberIndex, memberCountProperty, "memberCount")
		val memberIdArrayProperty = constructor.buildGetPropertyPointer(context.runtimeStructs.classDefinition, classDefinition,
			memberIdArrayPropertyIndex, "memberIdArrayProperty")
		val memberIdArray = constructor.buildLoad(constructor.pointerType, memberIdArrayProperty, "memberIdArray")
		if(Main.shouldPrintRuntimeDebugOutput) {
			// Assumption: Uninitialized memory is zeroed
			val isClassUninitialized = constructor.buildSignedIntegerEqualTo(memberIdArray, constructor.nullPointer,
				"isClassUninitialized")
			val panicBlock = constructor.createBlock(function, "uninitializedClassPanic")
			val initializedClassBlock = constructor.createBlock(function, "initializedClass")
			constructor.buildJump(isClassUninitialized, panicBlock, initializedClassBlock)
			constructor.select(panicBlock)
			context.panic(constructor, "Class definition at '%p' is uninitialized.", classDefinition)
			constructor.markAsUnreachable()
			constructor.select(initializedClassBlock)
		}
		val memberOffsetArrayProperty = constructor.buildGetPropertyPointer(context.runtimeStructs.classDefinition, classDefinition,
			memberOffsetArrayPropertyIndex, "memberOffsetArrayProperty")
		val memberOffsetArray = constructor.buildLoad(constructor.pointerType, memberOffsetArrayProperty, "memberOffsetArray")
		val indexVariable = constructor.buildStackAllocation(constructor.i32Type, "indexVariable")
		constructor.buildStore(constructor.buildInt32(0), indexVariable)
		val loopBlock = constructor.createBlock(function, "loop")
		constructor.buildJump(loopBlock)
		constructor.select(loopBlock)
		val currentIndex = constructor.buildLoad(context.runtimeTypes.memberIndex, indexVariable, "currentIndex")
		if(Main.shouldPrintRuntimeDebugOutput) {
			val isOutOfBounds = constructor.buildSignedIntegerEqualTo(currentIndex, memberCount, "isOutOfBounds")
			val panicBlock = constructor.createBlock(function, "outOfBoundsPanic")
			val idCheckBlock = constructor.createBlock(function, "idCheck")
			constructor.buildJump(isOutOfBounds, panicBlock, idCheckBlock)
			constructor.select(panicBlock)
			val targetMemberIdentifier = context.resolveMemberIdentifier(constructor, targetMemberId)
			context.panic(constructor, "$type '%s' with ID '%i' does not exist.", targetMemberIdentifier, targetMemberId)
			constructor.markAsUnreachable()
			constructor.select(idCheckBlock)
		}
		val newIndex = constructor.buildIntegerAddition(currentIndex, constructor.buildInt32(1), "newIndex")
		constructor.buildStore(newIndex, indexVariable)
		val currentIdElement = constructor.buildGetArrayElementPointer(context.runtimeTypes.memberId, memberIdArray, currentIndex,
			"currentIdElement")
		val currentId = constructor.buildLoad(context.runtimeTypes.memberId, currentIdElement, "currentId")
		val isMemberFound = constructor.buildSignedIntegerEqualTo(currentId, targetMemberId, "isMemberFound")
		val returnBlock = constructor.createBlock(function, "return")
		constructor.buildJump(isMemberFound, returnBlock, loopBlock)
		constructor.select(returnBlock)
		val memberOffsetElement =
			constructor.buildGetArrayElementPointer(context.runtimeTypes.memberOffset, memberOffsetArray, currentIndex,
				"memberOffsetElement")
		val memberOffset = constructor.buildLoad(context.runtimeTypes.memberOffset, memberOffsetElement, "memberOffset")
		if(Main.shouldPrintRuntimeDebugOutput) {
			val targetMemberIdentifier = context.resolveMemberIdentifier(constructor, targetMemberId)
			context.printDebugLine(constructor, "Found ${type.lowercase()} '%s' with offset '%i'.", targetMemberIdentifier,
				memberOffset)
		}
		constructor.buildReturn(memberOffset)
		if(type == "Constant")
			constantOffsetResolution = LlvmFunction(function, functionType)
		else
			propertyOffsetResolution = LlvmFunction(function, functionType)
	}

	private fun buildFunctionResolutionFunction(constructor: LlvmConstructor, context: Context) {
		val classPointerType = constructor.pointerType
		val functionType =
			constructor.buildFunctionType(listOf(classPointerType, context.runtimeTypes.memberId), context.runtimeTypes.memberAddress)
		val function = constructor.buildFunction("${RUNTIME_PREFIX}getFunctionAddress", functionType)
		constructor.createAndSelectEntrypointBlock(function)
		val classDefinition = constructor.getParameter(function, 0)
		val targetFunctionId = constructor.getParameter(function, 1)
		if(Main.shouldPrintRuntimeDebugOutput) {
			val targetFunctionIdentifier = context.resolveMemberIdentifier(constructor, targetFunctionId)
			context.printDebugLine(constructor, "Searching for function '%s' in class definition at '%p'.",
				targetFunctionIdentifier, classDefinition)
		}
		val functionCountProperty = constructor.buildGetPropertyPointer(context.runtimeStructs.classDefinition, classDefinition,
			Context.FUNCTION_COUNT_PROPERTY_INDEX, "functionCountProperty")
		val functionCount = constructor.buildLoad(context.runtimeTypes.memberIndex, functionCountProperty, "functionCount")
		val functionIdArrayProperty = constructor.buildGetPropertyPointer(context.runtimeStructs.classDefinition, classDefinition,
			Context.FUNCTION_ID_ARRAY_PROPERTY_INDEX, "functionIdArrayProperty")
		val functionIdArray = constructor.buildLoad(constructor.pointerType, functionIdArrayProperty, "functionIdArray")
		if(Main.shouldPrintRuntimeDebugOutput) {
			// Assumption: Uninitialized memory is zeroed
			val isClassUninitialized = constructor.buildSignedIntegerEqualTo(functionIdArray, constructor.nullPointer,
				"isClassUninitialized")
			val panicBlock = constructor.createBlock(function, "uninitializedClassPanic")
			val initializedClassBlock = constructor.createBlock(function, "initializedClass")
			constructor.buildJump(isClassUninitialized, panicBlock, initializedClassBlock)
			constructor.select(panicBlock)
			context.panic(constructor, "Class definition at '%p' is uninitialized.", classDefinition)
			constructor.markAsUnreachable()
			constructor.select(initializedClassBlock)
		}
		val functionAddressArrayProperty = constructor.buildGetPropertyPointer(context.runtimeStructs.classDefinition, classDefinition,
			Context.FUNCTION_ADDRESS_ARRAY_PROPERTY_INDEX, "functionAddressArrayProperty")
		val functionAddressArray = constructor.buildLoad(constructor.pointerType, functionAddressArrayProperty,
			"functionAddressArray")
		val indexVariable = constructor.buildStackAllocation(constructor.i32Type, "indexVariable")
		constructor.buildStore(constructor.buildInt32(0), indexVariable)
		val loopBlock = constructor.createBlock(function, "loop")
		constructor.buildJump(loopBlock)
		constructor.select(loopBlock)
		val currentIndex = constructor.buildLoad(context.runtimeTypes.memberIndex, indexVariable, "currentIndex")
		if(Main.shouldPrintRuntimeDebugOutput) {
			val isOutOfBounds = constructor.buildSignedIntegerEqualTo(currentIndex, functionCount, "isOutOfBounds")
			val panicBlock = constructor.createBlock(function, "outOfBoundsPanic")
			val idCheckBlock = constructor.createBlock(function, "idCheck")
			constructor.buildJump(isOutOfBounds, panicBlock, idCheckBlock)
			constructor.select(panicBlock)
			val targetFunctionIdentifier = context.resolveMemberIdentifier(constructor, targetFunctionId)
			context.panic(constructor, "Function '%s' does not exist.", targetFunctionIdentifier)
			constructor.markAsUnreachable()
			constructor.select(idCheckBlock)
		}
		val newIndex = constructor.buildIntegerAddition(currentIndex, constructor.buildInt32(1), "newIndex")
		constructor.buildStore(newIndex, indexVariable)
		val currentIdElement = constructor.buildGetArrayElementPointer(context.runtimeTypes.memberId, functionIdArray, currentIndex,
			"currentIdElement")
		val currentId = constructor.buildLoad(context.runtimeTypes.memberId, currentIdElement, "currentId")
		val isFunctionFound = constructor.buildSignedIntegerEqualTo(currentId, targetFunctionId, "isFunctionFound")
		val returnBlock = constructor.createBlock(function, "return")
		constructor.buildJump(isFunctionFound, returnBlock, loopBlock)
		constructor.select(returnBlock)
		val functionAddressElement = constructor.buildGetArrayElementPointer(context.runtimeTypes.memberAddress, functionAddressArray,
			currentIndex, "functionAddressElement")
		val functionAddress = constructor.buildLoad(context.runtimeTypes.memberAddress, functionAddressElement, "functionAddress")
		if(Main.shouldPrintRuntimeDebugOutput) {
			val targetFunctionIdentifier = context.resolveMemberIdentifier(constructor, targetFunctionId)
			context.printDebugLine(constructor, "Found function '%s' with address '%p'.", targetFunctionIdentifier,
				functionAddress)
		}
		constructor.buildReturn(functionAddress)
		functionAddressResolution = LlvmFunction(function, functionType)
	}

	//TODO this function is not calling the pre-initializer. Either:
	// - call it for String and ByteArray
	// - add comments marking this as an optimization, because the pre-initializer is empty
	//   - consider generalizing and automating this optimization
	// also: search project for other missed pre-initializer calls
	// also: this doesn't check for exceptions - same choice as above applies
	private fun buildCreateStringFunction(constructor: LlvmConstructor, context: Context) {
		val functionType = constructor.buildFunctionType(listOf(constructor.pointerType, constructor.pointerType, constructor.i32Type),
			constructor.pointerType)
		val function = constructor.buildFunction("${RUNTIME_PREFIX}createString", functionType)
		constructor.createAndSelectEntrypointBlock(function)
		val exceptionParameter = context.getExceptionParameter(constructor)
		val charArray = constructor.getParameter(1)
		val length = constructor.getParameter(2)

		val byteArrayRuntimeClass = context.standardLibrary.byteArray
		val byteArray = constructor.buildHeapAllocation(byteArrayRuntimeClass.struct, "_byteArray")
		byteArrayRuntimeClass.setClassDefinition(constructor, byteArray)
		val arraySizeProperty = context.resolveMember(constructor, byteArray, "size")
		constructor.buildStore(length, arraySizeProperty)

		val arrayValueProperty = byteArrayRuntimeClass.getNativeValueProperty(constructor, byteArray)
		constructor.buildStore(charArray, arrayValueProperty)

		val string = constructor.buildHeapAllocation(context.standardLibrary.stringTypeDeclaration?.llvmType, "_string")
		val stringClassDefinitionProperty =
			constructor.buildGetPropertyPointer(context.standardLibrary.stringTypeDeclaration?.llvmType, string,
				Context.CLASS_DEFINITION_PROPERTY_INDEX, "_stringClassDefinitionProperty")
		val stringClassDefinition = context.standardLibrary.stringTypeDeclaration?.llvmClassDefinition
			?: throw CompilerError("Missing string type declaration.")
		constructor.buildStore(stringClassDefinition, stringClassDefinitionProperty)
		val parameters = listOf(exceptionParameter, string, byteArray)
		constructor.buildFunctionCall(context.standardLibrary.stringByteArrayInitializer, parameters)
		constructor.buildReturn(string)
		createString = LlvmFunction(function, functionType)
	}

	private fun buildAddExceptionLocationFunction(constructor: LlvmConstructor, context: Context) {
		val functionType = constructor.buildFunctionType(
			listOf(constructor.pointerType, constructor.pointerType, constructor.i32Type, constructor.pointerType, constructor.i32Type,
				constructor.pointerType, constructor.i32Type, constructor.i32Type))
		val function = constructor.buildFunction("${RUNTIME_PREFIX}addExceptionLocation", functionType)
		constructor.createAndSelectEntrypointBlock(function)
		val exception = context.getExceptionParameter(constructor)
		val moduleNameBytes = constructor.getParameter(1)
		val moduleNameLength = constructor.getParameter(2)
		val fileNameBytes = constructor.getParameter(3)
		val fileNameLength = constructor.getParameter(4)
		val descriptionBytes = constructor.getParameter(5)
		val descriptionLength = constructor.getParameter(6)
		val lineNumber = constructor.getParameter(7)

		val ignoredExceptionVariable = constructor.buildStackAllocation(constructor.pointerType, "ignoredExceptionVariable")
		constructor.buildStore(constructor.nullPointer, ignoredExceptionVariable)
		val moduleName =
			constructor.buildFunctionCall(createString, listOf(ignoredExceptionVariable, moduleNameBytes, moduleNameLength), "moduleName")
		val fileName =
			constructor.buildFunctionCall(createString, listOf(ignoredExceptionVariable, fileNameBytes, fileNameLength), "fileName")
		val descriptionString =
			constructor.buildFunctionCall(createString, listOf(ignoredExceptionVariable, descriptionBytes, descriptionLength),
				"description")

		val addLocationFunctionAddress = context.resolveFunction(constructor, exception, "addLocation(String, String, Int, String)")
		constructor.buildFunctionCall(context.standardLibrary.exceptionAddLocationFunctionType, addLocationFunctionAddress,
			listOf(ignoredExceptionVariable, exception, moduleName, fileName, lineNumber, descriptionString))
		constructor.buildReturn()
		addExceptionLocation = LlvmFunction(function, functionType)
	}
}
