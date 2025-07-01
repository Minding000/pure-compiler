package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context

class ProcessNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("Process.getEnvironmentVariables(): <String, String>Map", ::getEnvironmentVariables)
		registry.registerNativeImplementation("Process.getArguments(): <String>Array", ::getArguments)
		registry.registerNativeImplementation("Process.getStandardInputStream(): NativeInputStream", ::getStandardInputStream)
		registry.registerNativeImplementation("Process.getStandardOutputStream(): NativeOutputStream", ::getStandardOutputStream)
		registry.registerNativeImplementation("Process.getStandardErrorStream(): NativeOutputStream", ::getStandardErrorStream)
	}

	private fun getEnvironmentVariables(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionParameter = context.getExceptionParameter(constructor, llvmFunctionValue)

		val mapTypeDeclaration = context.standardLibrary.mapTypeDeclaration
		val map = constructor.buildHeapAllocation(mapTypeDeclaration.llvmType, "environmentVariables")
		val mapClassDefinitionProperty =
			constructor.buildGetPropertyPointer(mapTypeDeclaration.llvmType, map,
				Context.CLASS_DEFINITION_PROPERTY_INDEX, "mapClassDefinitionProperty")
		constructor.buildStore(mapTypeDeclaration.llvmClassDefinition, mapClassDefinitionProperty)
		if(!mapTypeDeclaration.commonClassPreInitializer.isNoop) {
			val stringType = context.standardLibrary.stringTypeDeclaration.staticValueDeclaration?.llvmLocation
			constructor.buildFunctionCall(mapTypeDeclaration.commonClassPreInitializer,
				listOf(exceptionParameter, map, stringType, stringType)) //TODO requires target pre-initializer to be built already
		}
		val parameters = listOf(exceptionParameter, map)
		constructor.buildFunctionCall(context.standardLibrary.mapInitializer, parameters)

		val targetTriple = constructor.getTargetTriple()
		val asciiEqualSignCode = constructor.buildInt32(61)
		if(targetTriple.contains("windows")) {
			val environmentStringBlock =
				constructor.buildFunctionCall(context.externalFunctions.windowsGetEnvironmentStrings, listOf(), "environmentStringBlock")
			val environmentStringPointerVariable =
				constructor.buildStackAllocation(constructor.pointerType, "environmentStringPointerVariable", environmentStringBlock)
			val conditionBlock = constructor.createBlock("condition")
			val bodyBlock = constructor.createBlock("body")
			val exitBlock = constructor.createBlock("exit")
			constructor.buildJump(conditionBlock)
			constructor.select(conditionBlock)
			val environmentStringPointer =
				constructor.buildLoad(constructor.pointerType, environmentStringPointerVariable, "environmentStringPointer")
			val firstEnvironmentStringByte =
				constructor.buildLoad(constructor.byteType, environmentStringPointer, "firstEnvironmentStringByte")
			val isDone = constructor.buildSignedIntegerEqualTo(firstEnvironmentStringByte, constructor.buildByte(0), "isDone")
			constructor.buildJump(isDone, exitBlock, bodyBlock)
			constructor.select(bodyBlock)
			val delimiterPointer = constructor.buildFunctionCall(context.externalFunctions.stringSearchCharacter,
				listOf(environmentStringPointer, asciiEqualSignCode), "delimiterPointer")
			val value = constructor.buildGetArrayElementPointer(constructor.byteType, delimiterPointer, constructor.buildInt32(1), "value")
			val keyLength =
				constructor.buildPointerDifference(constructor.byteType, delimiterPointer, environmentStringPointer, "keyLength")
			val castKeyLength = constructor.buildCastFromLongToInteger(keyLength, "castKeyLength")
			val keyString = constructor.buildFunctionCall(context.runtimeFunctions.createString,
				listOf(exceptionParameter, environmentStringPointer, castKeyLength), "keyString")
			val valueLength = constructor.buildFunctionCall(context.externalFunctions.stringLength, listOf(value), "valueLength")
			val castValueLength = constructor.buildCastFromLongToInteger(valueLength, "castValueLength")
			val valueString =
				constructor.buildFunctionCall(context.runtimeFunctions.createString, listOf(exceptionParameter, value, castValueLength),
					"valueString")
			val mapSetterFunctionAddress = context.resolveFunction(constructor, map, "[Key](Value)")
			constructor.buildFunctionCall(context.standardLibrary.mapSetterFunctionType, mapSetterFunctionAddress,
				listOf(exceptionParameter, map, keyString, valueString))
			val nextEnvironmentStringOffset =
				constructor.buildIntegerAddition(castValueLength, constructor.buildInt32(1), "nextEnvironmentStringOffset")
			val newEnvironmentStringPointer =
				constructor.buildGetArrayElementPointer(constructor.byteType, value, nextEnvironmentStringOffset,
					"newEnvironmentStringPointer")
			constructor.buildStore(newEnvironmentStringPointer, environmentStringPointerVariable)
			constructor.buildJump(conditionBlock)
			constructor.select(exitBlock)
			//TODO call
			//constructor.buildFunctionCall(context.externalFunctions.windowsFreeEnvironmentStrings, listOf(environmentStringBlock))
			constructor.buildReturn(map)
		} else {
			val environmentStringArray =
				constructor.buildLoad(constructor.pointerType, context.runtimeGlobals.environmentStringArray, "environmentStringArray")
			val indexVariable = constructor.buildStackAllocation(constructor.i32Type, "indexVariable", constructor.buildInt32(0))
			val conditionBlock = constructor.createBlock("condition")
			val bodyBlock = constructor.createBlock("body")
			val exitBlock = constructor.createBlock("exit")
			constructor.buildJump(conditionBlock)
			constructor.select(conditionBlock)
			val currentIndex = constructor.buildLoad(constructor.i32Type, indexVariable, "index")
			val currentElement =
				constructor.buildGetArrayElementPointer(constructor.pointerType, environmentStringArray, currentIndex, "currentElement")
			val currentEnvironmentString = constructor.buildLoad(constructor.pointerType, currentElement, "currentEnvironmentString")
			val isDone = constructor.buildIsNull(currentEnvironmentString, "isDone")
			constructor.buildJump(isDone, exitBlock, bodyBlock)
			constructor.select(bodyBlock)
			val delimiterPointer = constructor.buildFunctionCall(context.externalFunctions.stringSearchCharacter,
				listOf(currentEnvironmentString, asciiEqualSignCode), "delimiterPointer")
			val value = constructor.buildGetArrayElementPointer(constructor.byteType, delimiterPointer, constructor.buildInt32(1), "value")
			val keyLength =
				constructor.buildPointerDifference(constructor.byteType, delimiterPointer, currentEnvironmentString, "keyLength")
			val castKeyLength = constructor.buildCastFromLongToInteger(keyLength, "castKeyLength")
			val keyString = constructor.buildFunctionCall(context.runtimeFunctions.createString,
				listOf(exceptionParameter, currentEnvironmentString, castKeyLength), "keyString")
			val valueLength = constructor.buildFunctionCall(context.externalFunctions.stringLength, listOf(value), "valueLength")
			val castValueLength = constructor.buildCastFromLongToInteger(valueLength, "castValueLength")
			val valueString =
				constructor.buildFunctionCall(context.runtimeFunctions.createString, listOf(exceptionParameter, value, castValueLength),
					"valueString")
			val mapSetterFunctionAddress = context.resolveFunction(constructor, map, "[Key](Value)")
			constructor.buildFunctionCall(context.standardLibrary.mapSetterFunctionType, mapSetterFunctionAddress,
				listOf(exceptionParameter, map, keyString, valueString))
			val newIndex = constructor.buildIntegerAddition(currentIndex, constructor.buildInt32(1), "newIndex")
			constructor.buildStore(newIndex, indexVariable)
			constructor.buildJump(conditionBlock)
			constructor.select(exitBlock)
			constructor.buildReturn(map)
		}
	}

	private fun getArguments(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val exceptionParameter = context.getExceptionParameter(constructor, llvmFunctionValue)
		val argumentCount = constructor.buildLoad(constructor.i32Type, context.runtimeGlobals.programArgumentCount, "argumentCount")
		val programArgumentArray =
			constructor.buildLoad(constructor.pointerType, context.runtimeGlobals.programArgumentArray, "programArgumentArray")
		val arrayTypeDeclaration = context.standardLibrary.arrayTypeDeclaration
		val arrayRuntimeClass = context.standardLibrary.array
		val array = constructor.buildHeapAllocation(arrayRuntimeClass.struct, "array")
		arrayRuntimeClass.setClassDefinition(constructor, array)
		if(!arrayTypeDeclaration.commonClassPreInitializer.isNoop) {
			val stringType = context.standardLibrary.stringTypeDeclaration.staticValueDeclaration?.llvmLocation
			constructor.buildFunctionCall(arrayTypeDeclaration.commonClassPreInitializer, listOf(exceptionParameter, array, stringType))
		}
		val arraySizeProperty = context.resolveMember(constructor, array, "size")
		constructor.buildStore(argumentCount, arraySizeProperty)
		val nativeArray = constructor.buildHeapArrayAllocation(constructor.pointerType, argumentCount, "nativeArray")
		val nativeArrayProperty = arrayRuntimeClass.getNativeValueProperty(constructor, array)
		constructor.buildStore(nativeArray, nativeArrayProperty)

		val indexVariable = constructor.buildStackAllocation(constructor.i32Type, "indexVariable", constructor.buildInt32(0))
		val conditionBlock = constructor.createBlock("condition")
		val bodyBlock = constructor.createBlock("body")
		val exitBlock = constructor.createBlock("exit")
		constructor.buildJump(conditionBlock)
		constructor.select(conditionBlock)
		val currentIndex = constructor.buildLoad(constructor.i32Type, indexVariable, "index")
		val isDone = constructor.buildSignedIntegerEqualTo(currentIndex, argumentCount, "isDone")
		constructor.buildJump(isDone, exitBlock, bodyBlock)
		constructor.select(bodyBlock)
		val argumentElement =
			constructor.buildGetArrayElementPointer(constructor.pointerType, programArgumentArray, currentIndex, "argumentElement")
		val argument = constructor.buildLoad(constructor.pointerType, argumentElement, "argument")
		val targetElement = constructor.buildGetArrayElementPointer(constructor.pointerType, nativeArray, currentIndex, "targetElement")

		val argumentLength = constructor.buildFunctionCall(context.externalFunctions.stringLength, listOf(argument), "argumentLength")
		val castArgumentLength = constructor.buildCastFromLongToInteger(argumentLength, "castArgumentLength")
		val argumentString =
			constructor.buildFunctionCall(context.runtimeFunctions.createString, listOf(exceptionParameter, argument, castArgumentLength),
				"argumentString")
		constructor.buildStore(argumentString, targetElement)
		val newIndex = constructor.buildIntegerAddition(currentIndex, constructor.buildInt32(1), "newIndex")
		constructor.buildStore(newIndex, indexVariable)

		constructor.buildJump(conditionBlock)
		constructor.select(exitBlock)
		constructor.buildReturn(array)
	}

	private fun getStandardInputStream(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val runtimeClass = context.standardLibrary.nativeInputStream
		val newObject = constructor.buildHeapAllocation(runtimeClass.struct, "standardInputStream")
		runtimeClass.setClassDefinition(constructor, newObject)
		val handleProperty = runtimeClass.getNativeValueProperty(constructor, newObject)
		val handle = constructor.buildLoad(constructor.pointerType, context.runtimeGlobals.standardInputStream, "handle")
		constructor.buildStore(handle, handleProperty)
		constructor.buildReturn(newObject)
	}

	private fun getStandardOutputStream(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val runtimeClass = context.standardLibrary.nativeOutputStream
		val newObject = constructor.buildHeapAllocation(runtimeClass.struct, "standardOutputStream")
		runtimeClass.setClassDefinition(constructor, newObject)
		val handleProperty = runtimeClass.getNativeValueProperty(constructor, newObject)
		val handle = constructor.buildLoad(constructor.pointerType, context.runtimeGlobals.standardOutputStream, "handle")
		constructor.buildStore(handle, handleProperty)
		constructor.buildReturn(newObject)
	}

	private fun getStandardErrorStream(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val runtimeClass = context.standardLibrary.nativeOutputStream
		val newObject = constructor.buildHeapAllocation(runtimeClass.struct, "standardErrorStream")
		runtimeClass.setClassDefinition(constructor, newObject)
		val handleProperty = runtimeClass.getNativeValueProperty(constructor, newObject)
		val handle = constructor.buildLoad(constructor.pointerType, context.runtimeGlobals.standardErrorStream, "handle")
		constructor.buildStore(handle, handleProperty)
		constructor.buildReturn(newObject)
	}
}
