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
		val runtimeClass = context.standardLibrary.nativeInputStream
		val newObject = constructor.buildHeapAllocation(runtimeClass.struct, "standardInputStream")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(runtimeClass.struct, newObject,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionProperty")
		constructor.buildStore(runtimeClass.classDefinition, classDefinitionProperty)
		val handleProperty = runtimeClass.getNativeValueProperty(constructor, newObject)
		val handle = constructor.buildLoad(constructor.pointerType, context.runtimeGlobals.standardInputStream, "handle")
		constructor.buildStore(handle, handleProperty)
		constructor.buildReturn(newObject)
	}

	private fun getStandardOutputStream(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val runtimeClass = context.standardLibrary.nativeOutputStream
		val newObject = constructor.buildHeapAllocation(runtimeClass.struct, "standardOutputStream")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(runtimeClass.struct, newObject,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionProperty")
		constructor.buildStore(runtimeClass.classDefinition, classDefinitionProperty)
		val handleProperty = runtimeClass.getNativeValueProperty(constructor, newObject)
		val handle = constructor.buildLoad(constructor.pointerType, context.runtimeGlobals.standardOutputStream, "handle")
		constructor.buildStore(handle, handleProperty)
		constructor.buildReturn(newObject)
	}

	private fun getStandardErrorStream(constructor: LlvmConstructor, llvmFunctionValue: LlvmValue) {
		constructor.createAndSelectEntrypointBlock(llvmFunctionValue)
		val runtimeClass = context.standardLibrary.nativeOutputStream
		val newObject = constructor.buildHeapAllocation(runtimeClass.struct, "standardErrorStream")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(runtimeClass.struct, newObject,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionProperty")
		constructor.buildStore(runtimeClass.classDefinition, classDefinitionProperty)
		val handleProperty = runtimeClass.getNativeValueProperty(constructor, newObject)
		val handle = constructor.buildLoad(constructor.pointerType, context.runtimeGlobals.standardErrorStream, "handle")
		constructor.buildStore(handle, handleProperty)
		constructor.buildReturn(newObject)
	}
}
