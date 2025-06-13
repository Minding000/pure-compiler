package components.code_generation.llvm

import components.code_generation.llvm.models.declarations.Initializer
import components.code_generation.llvm.models.declarations.TypeDeclaration
import components.code_generation.llvm.models.general.Program
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmFunction
import components.code_generation.llvm.wrapper.LlvmType
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.general.Program.Companion.RUNTIME_PREFIX
import components.semantic_model.types.FunctionType
import components.semantic_model.values.Operator
import errors.internal.CompilerError

class StandardLibrary {
	lateinit var array: NativeRuntimeClass
	lateinit var boolean: NativeRuntimeClass
	lateinit var byteArray: NativeRuntimeClass
	lateinit var byte: NativeRuntimeClass
	lateinit var integer: NativeRuntimeClass
	lateinit var float: NativeRuntimeClass
	lateinit var nativeInputStream: NativeRuntimeClass
	lateinit var nativeOutputStream: NativeRuntimeClass

	lateinit var byteArrayTypeDeclaration: TypeDeclaration
	lateinit var exceptionTypeDeclaration: TypeDeclaration
	lateinit var exceptionDescriptionInitializer: LlvmFunction
	lateinit var stringTypeDeclaration: TypeDeclaration
	lateinit var stringByteArrayInitializer: LlvmFunction
	lateinit var mapTypeDeclaration: TypeDeclaration
	lateinit var mapInitializer: LlvmFunction
	var exceptionAddLocationFunctionType: LlvmType? = null
	var mapSetterFunctionType: LlvmType? = null

	fun load(constructor: LlvmConstructor, context: Context, program: Program) {
		findBooleanTypeDeclaration(constructor, context)
		findByteTypeDeclaration(constructor, context)
		findIntegerTypeDeclaration(constructor, context)
		findFloatTypeDeclaration(constructor, context)
		findStringTypeDeclaration(context, program)
		findExceptionTypeDeclaration(constructor, context, program)
		findMapTypeDeclaration(constructor, context, program)
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

	private fun findStringTypeDeclaration(context: Context,
										  program: Program) { //TODO get LLVM TypeDeclaration instead (see: Program.getEntrypoint)
		//val specialTypePath = context.nativeRegistry.specialTypePaths[SpecialType.STRING] ?: return
		//val file = program.getFile(specialTypePath) ?: return
		//val typeDeclaration = file.units.filterIsInstance<components.code_generation.llvm.models.declarations.TypeDeclaration>().find { declaration -> declaration.model.name == SpecialType.STRING.className }



		val fileScope = context.nativeRegistry.specialTypeScopes[SpecialType.STRING]
		val typeDeclaration = fileScope?.getTypeDeclaration(SpecialType.STRING.className) ?: return
		val byteArrayInitializer = typeDeclaration.unit.units.filterIsInstance<Initializer>().find { initializer ->
			val parameters = initializer.model.parameters
			if(parameters.size != 1) return@find false
			val firstParameter = parameters.first()
			firstParameter.isPropertySetter && firstParameter.name == "bytes"
		} ?: throw CompilerError(typeDeclaration.source, "Failed to find String byte array initializer.")
		stringTypeDeclaration = typeDeclaration.unit
		stringByteArrayInitializer = LlvmFunction(byteArrayInitializer.llvmValue, byteArrayInitializer.llvmType)
	}

	private fun findExceptionTypeDeclaration(constructor: LlvmConstructor, context: Context,
											 program: Program) { //TODO get LLVM TypeDeclaration instead
		val fileScope = context.nativeRegistry.specialTypeScopes[SpecialType.EXCEPTION] ?: return
		val typeDeclaration = fileScope.getTypeDeclaration(SpecialType.EXCEPTION.className) ?: return
		val descriptionInitializer = typeDeclaration.unit.units.filterIsInstance<Initializer>().find { initializerDefinition ->
			val parameters = initializerDefinition.model.parameters
			if(parameters.size != 1) return@find false
			val firstParameter = parameters.first()
			firstParameter.isPropertySetter && firstParameter.name == "description"
		} ?: throw CompilerError(typeDeclaration.source, "Failed to find Exception description initializer.")
		val exceptionAddLocationPropertyType = typeDeclaration.scope.getValueDeclaration("addLocation")?.type
		exceptionTypeDeclaration = typeDeclaration.unit
		exceptionDescriptionInitializer = LlvmFunction(descriptionInitializer.llvmValue, descriptionInitializer.llvmType)
		exceptionAddLocationFunctionType =
			(exceptionAddLocationPropertyType as? FunctionType)?.signatures?.firstOrNull()?.getLlvmType(constructor)
	}

	private fun findMapTypeDeclaration(constructor: LlvmConstructor, context: Context,
									   program: Program) { //TODO get LLVM TypeDeclaration instead
		val fileScope = context.nativeRegistry.specialTypeScopes[SpecialType.MAP] ?: return
		val typeDeclaration = fileScope.getTypeDeclaration(SpecialType.MAP.className) ?: return
		val defaultInitializer = typeDeclaration.unit.units.filterIsInstance<Initializer>().find { initializerDefinition ->
			initializerDefinition.model.parameters.isEmpty()
		}
			?: throw CompilerError(typeDeclaration.source, "Failed to find default Map initializer.")
		val mapSetterPropertyType = typeDeclaration.scope.getValueDeclaration(Operator.Kind.BRACKETS_SET.stringRepresentation)?.type
		mapTypeDeclaration = typeDeclaration.unit
		mapInitializer = LlvmFunction(defaultInitializer.llvmValue, defaultInitializer.llvmType)
		mapSetterFunctionType = (mapSetterPropertyType as? FunctionType)?.signatures?.firstOrNull()?.getLlvmType(constructor)
	}

	class NativeRuntimeClass(val struct: LlvmType, val classDefinition: LlvmValue, private val valuePropertyIndex: Int) {

		constructor(typeDeclaration: TypeDeclaration, valuePropertyIndex: Int):
			this(typeDeclaration.llvmType, typeDeclaration.llvmClassDefinition, valuePropertyIndex)

		fun setClassDefinition(constructor: LlvmConstructor, targetObject: LlvmValue) {
			val classDefinitionProperty = constructor.buildGetPropertyPointer(struct, targetObject, Context.CLASS_DEFINITION_PROPERTY_INDEX,
				"_classDefinitionProperty")
			constructor.buildStore(classDefinition, classDefinitionProperty)
		}

		fun getNativeValueProperty(constructor: LlvmConstructor, structPointer: LlvmValue): LlvmValue {
			return constructor.buildGetPropertyPointer(struct, structPointer, valuePropertyIndex, "_nativeValueProperty")
		}
	}
}
