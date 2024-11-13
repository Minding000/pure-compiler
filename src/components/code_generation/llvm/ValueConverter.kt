package components.code_generation.llvm

import components.code_generation.llvm.models.declarations.TypeDeclaration
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmType
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.general.SemanticModel
import components.semantic_model.types.OptionalType
import components.semantic_model.types.Type
import errors.internal.CompilerError
import java.util.*
import components.semantic_model.declarations.TypeDeclaration as TypeDeclarationModel

object ValueConverter {
	//TODO use this function in these places (write tests!):
	// - function call target
	// - function call parameter		- DONE
	// - initializer call parameter
	// - assignment source				- DONE
	// - declaration assignment source	- DONE
	// - binary operator target
	// - binary operator source			- DONE
	// - binary modification source		- DONE
	// - cast target
	// - null check target
	// - return source					- DONE

	@Deprecated("Provide generics information.")
	fun convertIfRequired(model: SemanticModel, constructor: LlvmConstructor, sourceValue: LlvmValue, sourceType: Type?, targetType: Type?,
						  conversion: InitializerDefinition? = null): LlvmValue {
		return convertIfRequired(model, constructor, sourceValue, sourceType, false, targetType, false,
			conversion)
	}

	//TODO accept Unit as first parameter instead of SemanticModel
	fun convertIfRequired(model: SemanticModel, constructor: LlvmConstructor, sourceValue: LlvmValue, sourceType: Type?,
						  isSourceGeneric: Boolean, targetType: Type?, isTargetGeneric: Boolean,
						  conversion: InitializerDefinition? = null): LlvmValue {

		val sourceBaseType = if(sourceType is OptionalType) sourceType.baseType else sourceType
		val targetBaseType = if(targetType is OptionalType) targetType.baseType else targetType
		val isSourcePrimitive = !isSourceGeneric && sourceBaseType?.isLlvmPrimitive() != false
		val isTargetPrimitive = !isTargetGeneric && targetBaseType?.isLlvmPrimitive() != false

		if(sourceType !is OptionalType && isSourcePrimitive && targetType is OptionalType && isTargetPrimitive)
			return boxPrimitive(constructor, sourceValue, sourceType?.getLlvmType(constructor))
		if(sourceType is OptionalType && isSourcePrimitive && targetType !is OptionalType && isTargetPrimitive)
			return unboxPrimitive(constructor, sourceValue, targetType?.getLlvmType(constructor))
		if(isSourcePrimitive && !isTargetPrimitive && conversion == null)
			return wrapAndUnboxIfRequired(model, constructor, sourceValue, sourceType)
		if(!isSourcePrimitive && isTargetPrimitive && conversion == null)
			return unwrapAndBoxIfRequired(model, constructor, sourceValue, sourceType, targetType)
		if((SpecialType.BYTE.matches(sourceType) || SpecialType.INTEGER.matches(sourceType)) && SpecialType.FLOAT.matches(targetType)) {
			//TODO unbox / unwrap + box / wrap (write tests!)
			return constructor.buildCastFromSignedIntegerToFloat(sourceValue, "_castPrimitive")
		}
		if(SpecialType.BYTE.matches(sourceType) && SpecialType.INTEGER.matches(targetType)) {
			//TODO unbox / unwrap + box / wrap (write tests!)
			return constructor.buildCastFromByteToInteger(sourceValue, "_castPrimitive")
		}
		//TODO test (un-)box / (un-)wrap + conversion
		// - converting init accepts optional Int
		// - converting init accepts union with Int
		if(conversion != null)
			return buildConversion(model, constructor, sourceValue, conversion)
		return sourceValue
	}

	private fun wrapAndUnboxIfRequired(model: SemanticModel, constructor: LlvmConstructor, sourceValue: LlvmValue,
									   sourceType: Type?): LlvmValue {
		if(sourceType !is OptionalType)
			return wrapPrimitive(model, constructor, sourceValue, sourceType)
		val resultVariable = constructor.buildStackAllocation(constructor.pointerType, "_unboxAndWrap_resultVariable")
		val function = constructor.getParentFunction()
		val valueBlock = constructor.createBlock(function, "_unboxAndWrap_valueBlock")
		val nullBlock = constructor.createBlock(function, "_unboxAndWrap_nullBlock")
		val resultBlock = constructor.createBlock(function, "_unboxAndWrap_resultBlock")
		constructor.buildJump(constructor.buildIsNull(sourceValue, "_unboxAndWrap_isSourceNull"), nullBlock, valueBlock)
		constructor.select(nullBlock)
		constructor.buildStore(constructor.nullPointer, resultVariable)
		constructor.buildJump(resultBlock)
		constructor.select(valueBlock)
		val primitiveValue = unboxPrimitive(constructor, sourceValue, sourceType.baseType.getLlvmType(constructor))
		constructor.buildStore(wrapPrimitive(model, constructor, primitiveValue, sourceType.baseType), resultVariable)
		constructor.buildJump(resultBlock)
		constructor.select(resultBlock)
		return constructor.buildLoad(constructor.pointerType, resultVariable, "_unboxAndWrap_result")
	}

	private fun unwrapAndBoxIfRequired(model: SemanticModel, constructor: LlvmConstructor, sourceValue: LlvmValue, sourceType: Type?,
									   targetType: Type?): LlvmValue {
		if(targetType !is OptionalType)
			return unwrapPrimitive(model, constructor, sourceValue, targetType)
		val sourceLlvmType = sourceType?.getLlvmType(constructor)
		if(sourceType is OptionalType) {
			val resultVariable = constructor.buildStackAllocation(sourceLlvmType, "_unwrapAndBox_resultVariable")
			val function = constructor.getParentFunction()
			val valueBlock = constructor.createBlock(function, "_unwrapAndBox_valueBlock")
			val nullBlock = constructor.createBlock(function, "_unwrapAndBox_nullBlock")
			val resultBlock = constructor.createBlock(function, "_unwrapAndBox_resultBlock")
			constructor.buildJump(constructor.buildIsNull(sourceValue, "_unwrapAndBox_isSourceNull"), nullBlock, valueBlock)
			constructor.select(nullBlock)
			constructor.buildStore(constructor.nullPointer, resultVariable)
			constructor.buildJump(resultBlock)
			constructor.select(valueBlock)
			val primitiveValue = unwrapPrimitive(model, constructor, sourceValue, targetType.baseType)
			constructor.buildStore(boxPrimitive(constructor, primitiveValue, sourceLlvmType), resultVariable)
			constructor.buildJump(resultBlock)
			constructor.select(resultBlock)
			return constructor.buildLoad(sourceLlvmType, resultVariable, "_unwrapAndBox_result")
		} else {
			val primitiveValue = unwrapPrimitive(model, constructor, sourceValue, targetType.baseType)
			return boxPrimitive(constructor, primitiveValue, sourceLlvmType)
		}
	}

	private fun buildConversion(model: SemanticModel, constructor: LlvmConstructor, sourceValue: LlvmValue,
								conversion: InitializerDefinition): LlvmValue {
		val context = model.context
		val exceptionParameter = context.getExceptionParameter(constructor)
		val typeDeclaration = conversion.parentTypeDeclaration.unit
		val newObject = constructor.buildHeapAllocation(typeDeclaration.llvmType, "_newObject")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(typeDeclaration.llvmType, newObject,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "_classDefinitionProperty")
		constructor.buildStore(typeDeclaration.llvmClassDefinition, classDefinitionProperty)
		val parameters = LinkedList<LlvmValue?>()
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionParameter)
		parameters.add(Context.THIS_PARAMETER_INDEX, newObject)
		parameters.add(sourceValue)
		buildLlvmCommonPreInitializerCall(model, constructor, typeDeclaration, exceptionParameter, newObject)
		constructor.buildFunctionCall(conversion.unit.llvmType, conversion.unit.llvmValue, parameters)
		context.continueRaise(constructor, model)
		return newObject
	}

	private fun buildLlvmCommonPreInitializerCall(model: SemanticModel, constructor: LlvmConstructor, typeDeclaration: TypeDeclaration,
												  exceptionParameter: LlvmValue, newObject: LlvmValue) {
		val context = model.context
		val parameters = LinkedList<LlvmValue?>()
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionParameter)
		parameters.add(Context.THIS_PARAMETER_INDEX, newObject)
		constructor.buildFunctionCall(typeDeclaration.commonClassPreInitializer, parameters)
		context.continueRaise(constructor, model)
	}

	fun boxPrimitive(constructor: LlvmConstructor, value: LlvmValue, type: LlvmType?): LlvmValue {
		val box = constructor.buildHeapAllocation(type, "_optionalPrimitiveBox")
		constructor.buildStore(value, box)
		return box
	}

	private fun unboxPrimitive(constructor: LlvmConstructor, value: LlvmValue, type: LlvmType?): LlvmValue {
		return constructor.buildLoad(type, value, "_unboxedPrimitive")
	}

	fun wrapPrimitive(model: SemanticModel, constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue, type: Type?): LlvmValue {
		val context = model.context
		return if(SpecialType.BOOLEAN.matches(type)) {
			wrapBool(context, constructor, primitiveLlvmValue)
		} else if(SpecialType.BYTE.matches(type)) {
			wrapByte(context, constructor, primitiveLlvmValue)
		} else if(SpecialType.INTEGER.matches(type)) {
			wrapInteger(context, constructor, primitiveLlvmValue)
		} else if(SpecialType.FLOAT.matches(type)) {
			wrapFloat(context, constructor, primitiveLlvmValue)
		} else {
			throw CompilerError(model.source, "Unknown primitive type '$type'.")
		}
	}

	fun unwrapPrimitive(model: SemanticModel, constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue, type: Type?): LlvmValue {
		val context = model.context
		return if(SpecialType.BOOLEAN.matches(type)) {
			unwrapBool(context, constructor, wrappedLlvmValue)
		} else if(SpecialType.BYTE.matches(type)) {
			unwrapByte(context, constructor, wrappedLlvmValue)
		} else if(SpecialType.INTEGER.matches(type)) {
			unwrapInteger(context, constructor, wrappedLlvmValue)
		} else if(SpecialType.FLOAT.matches(type)) {
			unwrapFloat(context, constructor, wrappedLlvmValue)
		} else {
			throw CompilerError(model.source, "Unknown primitive type '$type'.")
		}
	}

	fun unwrapPrimitive(model: SemanticModel, constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue,
						typeDeclaration: TypeDeclarationModel?): LlvmValue {
		val context = model.context
		return if(SpecialType.BOOLEAN.matches(typeDeclaration)) {
			unwrapBool(context, constructor, wrappedLlvmValue)
		} else if(SpecialType.BYTE.matches(typeDeclaration)) {
			unwrapByte(context, constructor, wrappedLlvmValue)
		} else if(SpecialType.INTEGER.matches(typeDeclaration)) {
			unwrapInteger(context, constructor, wrappedLlvmValue)
		} else if(SpecialType.FLOAT.matches(typeDeclaration)) {
			unwrapFloat(context, constructor, wrappedLlvmValue)
		} else {
			throw CompilerError(model.source, "Unknown primitive type declaration '${typeDeclaration?.name}'.")
		}
	}

	fun wrapBool(context: Context, constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue): LlvmValue {
		val runtimeClass = context.standardLibrary.boolean
		val bool = constructor.buildHeapAllocation(runtimeClass.struct, "_bool")
		runtimeClass.setClassDefinition(constructor, bool)
		val valueProperty = runtimeClass.getNativeValueProperty(constructor, bool)
		constructor.buildStore(primitiveLlvmValue, valueProperty)
		return bool
	}

	fun unwrapBool(context: Context, constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val valueProperty = context.standardLibrary.boolean.getNativeValueProperty(constructor, wrappedLlvmValue)
		return constructor.buildLoad(constructor.booleanType, valueProperty, "_value")
	}

	fun wrapByte(context: Context, constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue): LlvmValue {
		val runtimeClass = context.standardLibrary.byte
		val byte = constructor.buildHeapAllocation(runtimeClass.struct, "_byte")
		runtimeClass.setClassDefinition(constructor, byte)
		val valueProperty = runtimeClass.getNativeValueProperty(constructor, byte)
		constructor.buildStore(primitiveLlvmValue, valueProperty)
		return byte
	}

	fun unwrapByte(context: Context, constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val valueProperty = context.standardLibrary.byte.getNativeValueProperty(constructor, wrappedLlvmValue)
		return constructor.buildLoad(constructor.byteType, valueProperty, "_value")
	}

	fun wrapInteger(context: Context, constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue): LlvmValue {
		val runtimeClass = context.standardLibrary.integer
		val integer = constructor.buildHeapAllocation(runtimeClass.struct, "_integer")
		runtimeClass.setClassDefinition(constructor, integer)
		val valueProperty = runtimeClass.getNativeValueProperty(constructor, integer)
		constructor.buildStore(primitiveLlvmValue, valueProperty)
		return integer
	}

	fun unwrapInteger(context: Context, constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val valueProperty = context.standardLibrary.integer.getNativeValueProperty(constructor, wrappedLlvmValue)
		return constructor.buildLoad(constructor.i32Type, valueProperty, "_value")
	}

	fun wrapFloat(context: Context, constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue): LlvmValue {
		val runtimeClass = context.standardLibrary.float
		val float = constructor.buildHeapAllocation(runtimeClass.struct, "_float")
		runtimeClass.setClassDefinition(constructor, float)
		val valueProperty = runtimeClass.getNativeValueProperty(constructor, float)
		constructor.buildStore(primitiveLlvmValue, valueProperty)
		return float
	}

	fun unwrapFloat(context: Context, constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val valueProperty = context.standardLibrary.float.getNativeValueProperty(constructor, wrappedLlvmValue)
		return constructor.buildLoad(constructor.floatType, valueProperty, "_value")
	}
}
