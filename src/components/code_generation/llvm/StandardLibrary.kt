package components.code_generation.llvm

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmFunction
import components.code_generation.llvm.wrapper.LlvmType
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.general.Program.Companion.RUNTIME_PREFIX
import components.semantic_model.types.FunctionType
import errors.internal.CompilerError
import kotlin.properties.Delegates

class StandardLibrary {
	lateinit var array: NativeRuntimeClass
	lateinit var boolean: NativeRuntimeClass
	lateinit var byteArray: NativeRuntimeClass
	lateinit var byte: NativeRuntimeClass
	lateinit var integer: NativeRuntimeClass
	lateinit var float: NativeRuntimeClass
	lateinit var nativeInputStream: NativeRuntimeClass
	lateinit var nativeOutputStream: NativeRuntimeClass

	var stringTypeDeclaration: TypeDeclaration? = null
	lateinit var stringByteArrayInitializer: LlvmFunction
	var exceptionAddLocationFunctionType: LlvmType? = null

	fun load(constructor: LlvmConstructor, context: Context) {
		findBooleanTypeDeclaration(constructor, context)
		findByteTypeDeclaration(constructor, context)
		findIntegerTypeDeclaration(constructor, context)
		findFloatTypeDeclaration(constructor, context)
		findStringInitializer(context)
		findExceptionAddLocationSignature(constructor, context)
	}

	private fun findBooleanTypeDeclaration(constructor: LlvmConstructor, context: Context) {
		val fileScope = context.nativeRegistry.specialTypeScopes[SpecialType.BOOLEAN]
		val typeDeclaration = fileScope?.getTypeDeclaration(SpecialType.BOOLEAN.className)
		if(typeDeclaration == null) {
			// Note: This is only here for compilation without the base library
			val struct = constructor.declareStruct("${RUNTIME_PREFIX}Bool")
			constructor.defineStruct(struct, listOf(constructor.pointerType, constructor.booleanType))
			boolean = NativeRuntimeClass(struct, constructor.nullPointer, 1)
		}
	}

	private fun findByteTypeDeclaration(constructor: LlvmConstructor, context: Context) {
		val fileScope = context.nativeRegistry.specialTypeScopes[SpecialType.BYTE]
		val typeDeclaration = fileScope?.getTypeDeclaration(SpecialType.BYTE.className)
		if(typeDeclaration == null) {
			// Note: This is only here for compilation without the base library
			val struct = constructor.declareStruct("${RUNTIME_PREFIX}Byte")
			constructor.defineStruct(struct, listOf(constructor.pointerType, constructor.byteType))
			byte = NativeRuntimeClass(struct, constructor.nullPointer, 1)
		}
	}

	private fun findIntegerTypeDeclaration(constructor: LlvmConstructor, context: Context) {
		val fileScope = context.nativeRegistry.specialTypeScopes[SpecialType.INTEGER]
		val typeDeclaration = fileScope?.getTypeDeclaration(SpecialType.INTEGER.className)
		if(typeDeclaration == null) {
			// Note: This is only here for compilation without the base library
			val struct = constructor.declareStruct("${RUNTIME_PREFIX}Int")
			constructor.defineStruct(struct, listOf(constructor.pointerType, constructor.i32Type))
			integer = NativeRuntimeClass(struct, constructor.nullPointer, 1)
		}
	}

	private fun findFloatTypeDeclaration(constructor: LlvmConstructor, context: Context) {
		val fileScope = context.nativeRegistry.specialTypeScopes[SpecialType.FLOAT]
		val typeDeclaration = fileScope?.getTypeDeclaration(SpecialType.FLOAT.className)
		if(typeDeclaration == null) {
			// Note: This is only here for compilation without the base library
			val struct = constructor.declareStruct("${RUNTIME_PREFIX}Float")
			constructor.defineStruct(struct, listOf(constructor.pointerType, constructor.floatType))
			float = NativeRuntimeClass(struct, constructor.nullPointer, 1)
		}
	}

	private fun findStringInitializer(context: Context) {
		val fileScope = context.nativeRegistry.specialTypeScopes[SpecialType.STRING]
		val typeDeclaration = fileScope?.getTypeDeclaration(SpecialType.STRING.className)
		stringTypeDeclaration = typeDeclaration
		if(typeDeclaration == null)
			return
		val byteArrayInitializer = typeDeclaration.getAllInitializers().find { initializerDefinition ->
			val parameters = initializerDefinition.parameters
			if(parameters.size != 1) return@find false
			val firstParameter = parameters.first()
			firstParameter.isPropertySetter && firstParameter.name == "bytes"
		} ?: throw CompilerError(typeDeclaration.source, "Failed to find String byte array initializer.")
		stringByteArrayInitializer = LlvmFunction(byteArrayInitializer.llvmValue, byteArrayInitializer.llvmType)
	}

	private fun findExceptionAddLocationSignature(constructor: LlvmConstructor, context: Context) {
		val fileScope = context.nativeRegistry.specialTypeScopes[SpecialType.EXCEPTION] ?: return
		val typeDeclaration = fileScope.getTypeDeclaration(SpecialType.EXCEPTION.className) ?: return
		val exceptionAddLocationPropertyType = typeDeclaration.scope.getValueDeclaration("addLocation")?.type
		exceptionAddLocationFunctionType =
			(exceptionAddLocationPropertyType as? FunctionType)?.signatures?.firstOrNull()?.getLlvmType(constructor)
	}

	class NativeRuntimeClass(val struct: LlvmType, val classDefinition: LlvmValue) {
		var valuePropertyIndex by Delegates.notNull<Int>()

		constructor(struct: LlvmType, classDefinition: LlvmValue, valuePropertyIndex: Int): this(struct, classDefinition) {
			this.valuePropertyIndex = valuePropertyIndex
		}

		constructor(typeDeclaration: TypeDeclaration): this(typeDeclaration.llvmType, typeDeclaration.llvmClassDefinition)

		constructor(typeDeclaration: TypeDeclaration, valuePropertyIndex: Int): this(typeDeclaration) {
			this.valuePropertyIndex = valuePropertyIndex
		}
	}
}
