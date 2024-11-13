package components.code_generation.llvm.native_implementations

import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context

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
