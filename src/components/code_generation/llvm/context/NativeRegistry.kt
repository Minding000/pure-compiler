package components.code_generation.llvm.context

import components.code_generation.llvm.native_implementations.*
import components.code_generation.llvm.native_implementations.primitives.PrimitiveIntNatives
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.scopes.FileScope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import logger.issues.declaration.MissingNativeImplementation

class NativeRegistry(val context: Context) {
	private val nativePrimitiveInitializers = HashMap<String, (constructor: LlvmConstructor, parameters: List<LlvmValue?>) -> LlvmValue>()
	private val nativeImplementations = HashMap<String, (constructor: LlvmConstructor, llvmValue: LlvmValue) -> Unit>()
	private val primitiveImplementations = HashMap<String, PrimitiveImplementation>()
	val specialTypeScopes = HashMap<SpecialType, FileScope>()
	val specialTypePaths = HashMap<SpecialType, List<String>>()

	fun loadNativeImplementations(constructor: LlvmConstructor) {
		PrimitiveIntNatives(context).load(this, constructor)

		ArrayNatives(context).load(this)
		BoolNatives(context).load(this)
		ByteArrayNatives(context).load(this)
		ByteNatives(context).load(this)
		ProcessNatives(context).load(this)
		FloatNatives(context).load(this)
		IdentifiableNatives(context).load(this)
		IntNatives(context).load(this)
		MathNatives(context).load(this)
		NullNatives(context).load(this)
		NativeInputStreamNatives(context).load(this)
		NativeOutputStreamNatives(context).load(this)
		StringNatives(context).load(this)
	}

	fun registerNativePrimitiveInitializer(identifier: String,
										   instance: (constructor: LlvmConstructor, parameters: List<LlvmValue?>) -> LlvmValue) {
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
		compileNativeImplementation(constructor, function.source, function.memberType, function.toString(), llvmValue)
	}

	fun compileNativeImplementation(constructor: LlvmConstructor, initializer: InitializerDefinition, llvmValue: LlvmValue) {
		compileNativeImplementation(constructor, initializer.source, "initializer", initializer.toString(), llvmValue)
	}

	fun compileNativeImplementation(constructor: LlvmConstructor, source: SyntaxTreeNode, type: String, signature: String,
									llvmValue: LlvmValue) {
		val compileImplementation = nativeImplementations[signature]
		if(compileImplementation == null) {
			context.addIssue(MissingNativeImplementation(source, type, signature))
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

	fun has(vararg specialTypes: SpecialType): Boolean {
		return specialTypes.all { specialType -> specialTypeScopes.containsKey(specialType) }
	}
}
