package components.semantic_model.context

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.code_generation.llvm.native_implementations.*
import components.code_generation.llvm.native_implementations.primitives.PrimitiveIntNatives
import components.semantic_model.declarations.FunctionImplementation
import errors.internal.CompilerError
import logger.issues.declaration.MissingNativeImplementation

class NativeRegistry(val context: Context) {
	private val nativePrimitiveInitializers = HashMap<String, (constructor: LlvmConstructor, parameters: List<LlvmValue?>) -> LlvmValue>()
	private val nativeImplementations = HashMap<String, (constructor: LlvmConstructor, llvmValue: LlvmValue) -> Unit>()
	private val primitiveImplementations = HashMap<String, PrimitiveImplementation>()

	fun loadNativeImplementations(constructor: LlvmConstructor) {
		PrimitiveIntNatives.load(this, constructor)

		ArrayNatives.load(this)
		BoolNatives.load(this)
		ByteNatives.load(this)
		CliNatives.load(this)
		FloatNatives.load(this)
		IdentifiableNatives.load(this)
		IntNatives.load(this)
		NullNatives.load(this)
		//TODO currently unused
//		NativeInputStreamNatives.load(this)
//		NativeOutputStreamNatives.load(this)
	}

	fun registerNativePrimitiveInitializer(identifier: String, instance: (constructor: LlvmConstructor, parameters: List<LlvmValue?>) -> LlvmValue) {
		val existingInstance = nativePrimitiveInitializers.putIfAbsent(identifier, instance)
		if(existingInstance != null)
			throw CompilerError("Duplicate native primitive initializer for identifier '$identifier'.")
	}

	fun inlineNativePrimitiveInitializer(constructor: LlvmConstructor, identifier: String, parameters: List<LlvmValue?>): LlvmValue {
		val getPrimitiveInitializerResult = nativePrimitiveInitializers[identifier]
			?: throw CompilerError("Missing native primitive initializer for identifier '$identifier'.")
		return getPrimitiveInitializerResult(constructor, parameters)
	}

	fun registerNativeImplementation(identifier: String, implementation: (constructor: LlvmConstructor, llvmValue: LlvmValue) -> Unit) {
		val existingImplementation = nativeImplementations.putIfAbsent(identifier, implementation)
		if(existingImplementation != null)
			throw CompilerError("Duplicate native implementation for identifier '$identifier'.")
	}

	fun compileNativeImplementation(constructor: LlvmConstructor, function: FunctionImplementation, llvmValue: LlvmValue) {
		val compileImplementation = nativeImplementations[function.toString()]
		if(compileImplementation == null) {
			context.addIssue(MissingNativeImplementation(function))
			return
		}
		compileImplementation(constructor, llvmValue)
	}

	fun registerPrimitiveImplementation(identifier: String, implementation: PrimitiveImplementation) {
		val existingImplementation = primitiveImplementations.putIfAbsent(identifier, implementation)
		if(existingImplementation != null)
			throw CompilerError("Duplicate primitive implementation for identifier '$identifier'.")
	}

	fun resolvePrimitiveImplementation(identifier: String): PrimitiveImplementation {
		val primitiveImplementation = primitiveImplementations[identifier]
			?: throw CompilerError("Missing primitive implementation for identifier '$identifier'.")
		return primitiveImplementation
	}
}
