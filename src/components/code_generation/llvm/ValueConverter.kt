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
			val box = constructor.buildHeapAllocation(sourceType.getLlvmType(constructor), "_optionalPrimitiveBox")
			constructor.buildStore(sourceValue, box)
			return box
		}
		if(sourceType is OptionalType && targetType?.isLlvmPrimitive() == true) {
			return constructor.buildLoad(targetType.getLlvmType(constructor), sourceValue, "_unboxedPrimitive")
		}
		if(sourceType?.isLlvmPrimitive() == true && targetType?.isLlvmPrimitive() == false) {
			//TODO same for other primitives (write tests!):
			// - float
			// - bool
			// - byte
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
			//TODO same for other primitives (write tests!):
			// - float
			// - bool
			// - byte
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
}
