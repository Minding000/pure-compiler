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

	fun convertIfRequired(model: SemanticModel, constructor: LlvmConstructor, sourceValue: LlvmValue, sourceType: Type?,
						  targetType: Type?, conversion: InitializerDefinition? = null): LlvmValue {
		val source = model.source
		val context = model.context
		//TODO also handle optional primitive to wrapped (and vice-versa) cases
		// Wrapped optional -> Boxed primitive
		// Boxed primitive -> wrapped optional

		if(sourceType?.isLlvmPrimitive() == true && targetType is OptionalType) {
			return boxPrimitive(constructor, sourceValue, sourceType.getLlvmType(constructor))
		}
		if(sourceType is OptionalType && targetType?.isLlvmPrimitive() == true) {
			return unboxPrimitive(constructor, sourceValue, targetType.getLlvmType(constructor))
		}
		if(sourceType?.isLlvmPrimitive() == true && targetType?.isLlvmPrimitive() == false) {
			//TODO same for other primitives (write tests!):
			// - float
			// - bool
			// - byte
			if(SpecialType.INTEGER.matches(sourceType)) {
				return wrapInteger(context, constructor, sourceValue)
			} else {
				throw CompilerError(source, "Unknown primitive type '$sourceType'.")
			}
		}
		val sourceBaseType = if(sourceType is OptionalType) sourceType.baseType else sourceType
		val targetBaseType = if(targetType is OptionalType) targetType.baseType else targetType
		if(sourceBaseType?.isLlvmPrimitive() == false && targetBaseType?.isLlvmPrimitive() == true) {
			//TODO same for other primitives (write tests!):
			// - float
			// - bool
			// - byte
			if(SpecialType.INTEGER.matches(targetBaseType)) {
				if(targetType is OptionalType) {
					val sourceLlvmType = sourceType?.getLlvmType(constructor)
					if(sourceType is OptionalType) {
						val result = constructor.buildStackAllocation(sourceLlvmType, "_unwrapAndBoxResultVariable")
						val function = constructor.getParentFunction()
						val valueBlock = constructor.createBlock(function, "_unwrapAndBoxValueBlock")
						val nullBlock = constructor.createBlock(function, "_unwrapAndBoxNullBlock")
						val resultBlock = constructor.createBlock(function, "_unwrapAndBoxResultBlock")
						constructor.buildJump(constructor.buildIsNull(sourceValue, "_isSourceNull"), nullBlock, valueBlock)
						constructor.select(nullBlock)
						constructor.buildStore(constructor.nullPointer, result)
						constructor.buildJump(resultBlock)
						constructor.select(valueBlock)
						val integer = unwrapInteger(context, constructor, sourceValue)
						constructor.buildStore(boxPrimitive(constructor, integer, sourceLlvmType), result)
						constructor.buildJump(resultBlock)
						constructor.select(resultBlock)
						return result
					} else {
						val integer = unwrapInteger(context, constructor, sourceValue)
						return boxPrimitive(constructor, integer, sourceLlvmType)
					}
				}
				return unwrapInteger(context, constructor, sourceValue)
			} else {
				throw CompilerError(source, "Unknown primitive type '$targetBaseType'.")
			}
		}
		if(SpecialType.INTEGER.matches(sourceType) && SpecialType.FLOAT.matches(targetType)) {
			return constructor.buildCastFromSignedIntegerToFloat(sourceValue, "_castPrimitive")
		}
		if(conversion != null) {
			val exceptionAddressLocation = constructor.buildStackAllocation(constructor.pointerType, "exceptionAddress")
			val typeDeclaration = conversion.parentTypeDeclaration
			val newObjectAddress = constructor.buildHeapAllocation(typeDeclaration.llvmType, "newObjectAddress")
			val classDefinitionPointer = constructor.buildGetPropertyPointer(typeDeclaration.llvmType, newObjectAddress,
				Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinitionPointer")
			constructor.buildStore(typeDeclaration.llvmClassDefinitionAddress, classDefinitionPointer)
			val parameters = LinkedList<LlvmValue?>()
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddressLocation)
			parameters.add(Context.THIS_PARAMETER_INDEX, newObjectAddress)
			parameters.add(sourceValue)
			constructor.buildFunctionCall(conversion.llvmType, conversion.llvmValue, parameters)
			return newObjectAddress
		}
		return sourceValue
	}

	private fun boxPrimitive(constructor: LlvmConstructor, value: LlvmValue, type: LlvmType?): LlvmValue {
		val box = constructor.buildHeapAllocation(type, "_optionalPrimitiveBox")
		constructor.buildStore(value, box)
		return box
	}

	private fun unboxPrimitive(constructor: LlvmConstructor, value: LlvmValue, type: LlvmType?): LlvmValue {
		return constructor.buildLoad(type, value, "_unboxedPrimitive")
	}

	fun wrapInteger(context: Context, constructor: LlvmConstructor, primitiveLlvmValue: LlvmValue): LlvmValue {
		val newIntegerAddress = constructor.buildHeapAllocation(context.integerTypeDeclaration?.llvmType, "newIntegerAddress")
		val integerClassDefinitionPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType,
			newIntegerAddress, Context.CLASS_DEFINITION_PROPERTY_INDEX, "integerClassDefinitionPointer")
		val integerClassDefinitionAddress = context.integerTypeDeclaration?.llvmClassDefinitionAddress
			?: throw CompilerError("Missing integer type declaration.")
		constructor.buildStore(integerClassDefinitionAddress, integerClassDefinitionPointer)
		val valuePointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, newIntegerAddress,
			context.integerValueIndex, "valuePointer")
		constructor.buildStore(primitiveLlvmValue, valuePointer)
		return newIntegerAddress
	}

	fun unwrapInteger(context: Context, constructor: LlvmConstructor, wrappedLlvmValue: LlvmValue): LlvmValue {
		val propertyPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, wrappedLlvmValue,
			context.integerValueIndex, "_propertyPointer")
		return constructor.buildLoad(constructor.i32Type, propertyPointer, "_primitiveValue")
	}
}
