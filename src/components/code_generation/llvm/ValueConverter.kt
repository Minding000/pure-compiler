package components.code_generation.llvm

import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.general.SemanticModel
import components.semantic_model.types.OptionalType
import components.semantic_model.types.Type
import errors.internal.CompilerError

object ValueConverter {

	fun convertIfRequired(model: SemanticModel, constructor: LlvmConstructor, sourceValue: LlvmValue, sourceType: Type?,
						  targetType: Type?): LlvmValue {
		val source = model.source
		val context = model.context
		//TODO also handle optional primitive to wrapped (and vice-versa) cases
		// Wrapped optional -> Boxed primitive
		// Boxed primitive -> wrapped optional

		if(sourceType?.isLlvmPrimitive() == true && targetType is OptionalType) {
			val box = constructor.buildHeapAllocation(sourceType.getLlvmType(constructor), "_optionalPrimitiveBox")
			constructor.buildStore(sourceValue, box)
			return box
		}
		if(sourceType is OptionalType && targetType?.isLlvmPrimitive() == true) {
			return constructor.buildLoad(targetType.getLlvmType(constructor), sourceValue, "_unboxedPrimitive")
		}
		if(sourceType?.isLlvmPrimitive() == true && targetType?.isLlvmPrimitive() == false) {
			if(SpecialType.INTEGER.matches(sourceType)) {
				val newIntegerAddress = constructor.buildHeapAllocation(context.integerTypeDeclaration?.llvmType, "newIntegerAddress")
				val integerClassDefinitionPointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType,
					newIntegerAddress, Context.CLASS_DEFINITION_PROPERTY_INDEX, "integerClassDefinitionPointer")
				val integerClassDefinitionAddress = context.integerTypeDeclaration?.llvmClassDefinitionAddress
					?: throw CompilerError(source, "Missing integer type declaration.")
				constructor.buildStore(integerClassDefinitionAddress, integerClassDefinitionPointer)
				val valuePointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, newIntegerAddress,
					context.integerValueIndex, "valuePointer")
				constructor.buildStore(sourceValue, valuePointer)
				return newIntegerAddress
			} else {
				throw CompilerError(source, "Unknown primitive type '$sourceType'.")
			}
		}
		if(sourceType?.isLlvmPrimitive() == false && targetType?.isLlvmPrimitive() == true) {
			if(SpecialType.INTEGER.matches(targetType)) {
				val valuePointer = constructor.buildGetPropertyPointer(context.integerTypeDeclaration?.llvmType, sourceValue,
					context.integerValueIndex, "valuePointer")
				return constructor.buildLoad(targetType.getLlvmType(constructor), valuePointer, "_unwrappedPrimitive")
			} else {
				throw CompilerError(source, "Unknown primitive type '$sourceType'.")
			}
		}
		if(SpecialType.INTEGER.matches(sourceType) && SpecialType.FLOAT.matches(targetType)) {
			return constructor.buildCastFromSignedIntegerToFloat(sourceValue, "_castPrimitive")
		}
		//TODO implement custom converting initializer calls
		return sourceValue
	}
}
