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
				//TODO support unboxing and wrapping (like the opposite 'unwrapAndBox' below)
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
						val result = constructor.buildStackAllocation(sourceLlvmType, "_unwrapAndBox_resultVariable")
						val function = constructor.getParentFunction()
						val valueBlock = constructor.createBlock(function, "_unwrapAndBox_valueBlock")
						val nullBlock = constructor.createBlock(function, "_unwrapAndBox_nullBlock")
						val resultBlock = constructor.createBlock(function, "_unwrapAndBox_resultBlock")
						constructor.buildJump(constructor.buildIsNull(sourceValue, "_unwrapAndBox_isSourceNull"), nullBlock,
							valueBlock)
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
}
