package components.code_generation.llvm

import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.general.SemanticModel
import components.semantic_model.types.OptionalType
import components.semantic_model.types.Type
import errors.internal.CompilerError
import java.util.*

object ValueConverter {
	//TODO use this function in these places (write tests!):
	// - function call target
	// - function call parameter
	// - initializer call parameter
	// - assignment source
	// - declaration assignment source
	// - binary operator target
	// - binary operator source
	// - binary modification source
	// - cast target
	// - null check target
	// - return source

	fun convertIfRequired(model: SemanticModel, constructor: LlvmConstructor, sourceValue: LlvmValue, sourceType: Type?, targetType: Type?,
						  conversion: InitializerDefinition? = null): LlvmValue {
		//TODO test (un-)box / (un-)wrap + cast
		//TODO test (un-)box / (un-)wrap + conversion
		if(sourceType?.isLlvmPrimitive() == true && targetType is OptionalType)
			return boxPrimitive(constructor, sourceValue, sourceType.getLlvmType(constructor))
		if(sourceType is OptionalType && targetType?.isLlvmPrimitive() == true)
			return unboxPrimitive(constructor, sourceValue, targetType.getLlvmType(constructor))
		val sourceBaseType = if(sourceType is OptionalType) sourceType.baseType else sourceType
		val targetBaseType = if(targetType is OptionalType) targetType.baseType else targetType
		if(sourceType?.isLlvmPrimitive() == true && targetType?.isLlvmPrimitive() == false) {
			//TODO support unboxing and wrapping (like the opposite 'unwrapAndBox' below)
			return wrap(model, constructor, sourceValue, sourceType)
		}
		if(sourceBaseType?.isLlvmPrimitive() == false && targetBaseType?.isLlvmPrimitive() == true) {
			if(targetType !is OptionalType)
				return unwrap(model, constructor, sourceValue, targetBaseType)
			val sourceLlvmType = sourceType?.getLlvmType(constructor)
			if(sourceType is OptionalType) {
				val result = constructor.buildStackAllocation(sourceLlvmType, "_unwrapAndBox_resultVariable")
				val function = constructor.getParentFunction()
				val valueBlock = constructor.createBlock(function, "_unwrapAndBox_valueBlock")
				val nullBlock = constructor.createBlock(function, "_unwrapAndBox_nullBlock")
				val resultBlock = constructor.createBlock(function, "_unwrapAndBox_resultBlock")
				constructor.buildJump(constructor.buildIsNull(sourceValue, "_unwrapAndBox_isSourceNull"), nullBlock, valueBlock)
				constructor.select(nullBlock)
				constructor.buildStore(constructor.nullPointer, result)
				constructor.buildJump(resultBlock)
				constructor.select(valueBlock)
				val primitiveValue = unwrap(model, constructor, sourceValue, targetBaseType)
				constructor.buildStore(boxPrimitive(constructor, primitiveValue, sourceLlvmType), result)
				constructor.buildJump(resultBlock)
				constructor.select(resultBlock)
				return result
			} else {
				val primitiveValue = unwrap(model, constructor, sourceValue, targetBaseType)
				return boxPrimitive(constructor, primitiveValue, sourceLlvmType)
			}
		}
		if((SpecialType.BYTE.matches(sourceType) || SpecialType.INTEGER.matches(sourceType)) && SpecialType.FLOAT.matches(targetType))
			return constructor.buildCastFromSignedIntegerToFloat(sourceValue, "_castPrimitive")
		if(SpecialType.BYTE.matches(sourceType) && SpecialType.INTEGER.matches(targetType))
			return constructor.buildCastFromByteToInteger(sourceValue, "_castPrimitive")
		if(conversion != null)
			return buildConversion(constructor, sourceValue, conversion)
		return sourceValue
	}

	private fun buildConversion(constructor: LlvmConstructor, sourceValue: LlvmValue, conversion: InitializerDefinition): LlvmValue {
		val exceptionAddress = constructor.buildStackAllocation(constructor.pointerType, "__exceptionAddress")
		val typeDeclaration = conversion.parentTypeDeclaration
		val newObject = constructor.buildHeapAllocation(typeDeclaration.llvmType, "_newObject")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(typeDeclaration.llvmType, newObject,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "_classDefinitionProperty")
		constructor.buildStore(typeDeclaration.llvmClassDefinition, classDefinitionProperty)
		val parameters = LinkedList<LlvmValue?>()
		parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
		parameters.add(Context.THIS_PARAMETER_INDEX, newObject)
		parameters.add(sourceValue)
		constructor.buildFunctionCall(conversion.llvmType, conversion.llvmValue, parameters)
		return newObject
	}

	private fun boxPrimitive(constructor: LlvmConstructor, value: LlvmValue, type: LlvmType?): LlvmValue {
		val box = constructor.buildHeapAllocation(type, "_optionalPrimitiveBox")
		constructor.buildStore(value, box)
		return box
	}

	private fun unboxPrimitive(constructor: LlvmConstructor, value: LlvmValue, type: LlvmType?): LlvmValue {
		return constructor.buildLoad(type, value, "_unboxedPrimitive")
	}

	private fun wrap(model: SemanticModel, constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue, type: Type?): LlvmValue {
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

	private fun unwrap(model: SemanticModel, constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue, type: Type?): LlvmValue {
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

	fun wrapBool(context: Context, constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue): LlvmValue {
		val bool = constructor.buildHeapAllocation(context.booleanTypeDeclaration?.llvmType, "_bool")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(context.booleanTypeDeclaration?.llvmType, bool,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "_classDefinitionProperty")
		val classDefinition = context.booleanTypeDeclaration?.llvmClassDefinition
			?: throw CompilerError("Missing bool type declaration.")
		constructor.buildStore(classDefinition, classDefinitionProperty)
		val valueProperty = constructor.buildGetPropertyPointer(context.booleanTypeDeclaration?.llvmType, bool, context.booleanValueIndex,
			"_valueProperty")
		constructor.buildStore(primitiveLlvmValue, valueProperty)
		return bool
	}

	fun unwrapBool(context: Context, constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val valueProperty = constructor.buildGetPropertyPointer(context.booleanTypeDeclaration?.llvmType, wrappedLlvmValue,
			context.booleanValueIndex, "_valueProperty")
		return constructor.buildLoad(constructor.booleanType, valueProperty, "_value")
	}

	fun wrapByte(context: Context, constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue): LlvmValue {
		val byte = constructor.buildHeapAllocation(context.byteTypeDeclaration?.llvmType, "_byte")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(context.byteTypeDeclaration?.llvmType, byte,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "_classDefinitionProperty")
		val classDefinition = context.byteTypeDeclaration?.llvmClassDefinition
			?: throw CompilerError("Missing byte type declaration.")
		constructor.buildStore(classDefinition, classDefinitionProperty)
		val valueProperty = constructor.buildGetPropertyPointer(context.byteTypeDeclaration?.llvmType, byte, context.byteValueIndex,
			"_valueProperty")
		constructor.buildStore(primitiveLlvmValue, valueProperty)
		return byte
	}

	fun unwrapByte(context: Context, constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val valueProperty = constructor.buildGetPropertyPointer(context.byteTypeDeclaration?.llvmType, wrappedLlvmValue,
			context.byteValueIndex, "_valueProperty")
		return constructor.buildLoad(constructor.byteType, valueProperty, "_value")
	}

	fun wrapInteger(context: Context, constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue): LlvmValue {
		val integer = constructor.buildHeapAllocation(context.integerTypeDeclaration?.llvmType, "_integer")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, integer,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "_classDefinitionProperty")
		val classDefinition = context.integerTypeDeclaration?.llvmClassDefinition
			?: throw CompilerError("Missing integer type declaration.")
		constructor.buildStore(classDefinition, classDefinitionProperty)
		val valueProperty = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, integer,
			context.integerValueIndex, "_valueProperty")
		constructor.buildStore(primitiveLlvmValue, valueProperty)
		return integer
	}

	fun unwrapInteger(context: Context, constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val valueProperty = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, wrappedLlvmValue,
			context.integerValueIndex, "_valueProperty")
		return constructor.buildLoad(constructor.i32Type, valueProperty, "_value")
	}

	fun wrapFloat(context: Context, constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue): LlvmValue {
		val float = constructor.buildHeapAllocation(context.floatTypeDeclaration?.llvmType, "_float")
		val classDefinitionProperty = constructor.buildGetPropertyPointer(context.floatTypeDeclaration?.llvmType, float,
			Context.CLASS_DEFINITION_PROPERTY_INDEX, "_classDefinitionProperty")
		val classDefinition = context.floatTypeDeclaration?.llvmClassDefinition
			?: throw CompilerError("Missing float type declaration.")
		constructor.buildStore(classDefinition, classDefinitionProperty)
		val valueProperty = constructor.buildGetPropertyPointer(context.floatTypeDeclaration?.llvmType, float, context.floatValueIndex,
			"_valueProperty")
		constructor.buildStore(primitiveLlvmValue, valueProperty)
		return float
	}

	fun unwrapFloat(context: Context, constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val valueProperty = constructor.buildGetPropertyPointer(context.floatTypeDeclaration?.llvmType, wrappedLlvmValue,
			context.floatValueIndex, "_valueProperty")
		return constructor.buildLoad(constructor.floatType, valueProperty, "_value")
	}
}
