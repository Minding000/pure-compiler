package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.NativeRegistry

class ProcessNatives(val context: Context) {

	fun load(registry: NativeRegistry) {
		registry.registerNativeImplementation("Process.getStandardInputStream(): NativeInputStream", ::getStandardInputStream)
		registry.registerNativeImplementation("Process.getStandardOutputStream(): NativeOutputStream", ::getStandardOutputStream)
		registry.registerNativeImplementation("Process.getStandardErrorStream(): NativeOutputStream", ::getStandardErrorStream)
	}

	private fun getStandardInputStream(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val objectType = context.nativeInputStreamDeclarationType
		val newObject = constructor.buildHeapAllocation(objectType, "standardInputStream")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(objectType, newObject,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionProperty")
		constructor.buildStore(context.nativeInputStreamClassDefinition, classDefinitionProperty)
		val handleProperty = constructor.buildGetPropertyPointer(objectType, newObject,
			context.nativeInputStreamValueIndex, "handleProperty")
		val handle = constructor.buildLoad(constructor.pointerType, context.llvmStandardInputStreamGlobal, "handle")
		constructor.buildStore(handle, handleProperty)
		constructor.buildReturn(newObject)
	}

	private fun getStandardOutputStream(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val objectType = context.nativeOutputStreamDeclarationType
		val newObject = constructor.buildHeapAllocation(objectType, "standardOutputStream")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(objectType, newObject,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionProperty")
		constructor.buildStore(context.nativeOutputStreamClassDefinition, classDefinitionProperty)
		val handleProperty = constructor.buildGetPropertyPointer(objectType, newObject,
			context.nativeOutputStreamValueIndex, "handleProperty")
		val handle = constructor.buildLoad(constructor.pointerType, context.llvmStandardOutputStreamGlobal, "handle")
		constructor.buildStore(handle, handleProperty)
		constructor.buildReturn(newObject)
	}

	private fun getStandardErrorStream(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val objectType = context.nativeOutputStreamDeclarationType
		val newObject = constructor.buildHeapAllocation(objectType, "standardErrorStream")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(objectType, newObject,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionProperty")
		constructor.buildStore(context.nativeOutputStreamClassDefinition, classDefinitionProperty)
		val handleProperty = constructor.buildGetPropertyPointer(objectType, newObject,
			context.nativeOutputStreamValueIndex, "handleProperty")
		val handle = constructor.buildLoad(constructor.pointerType, context.llvmStandardErrorStreamGlobal, "handle")
		constructor.buildStore(handle, handleProperty)
		constructor.buildReturn(newObject)
	}
}
