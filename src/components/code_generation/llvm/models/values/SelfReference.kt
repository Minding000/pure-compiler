package components.code_generation.llvm.models.values

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.values.SelfReference
import errors.internal.CompilerError

open class SelfReference(override val model: SelfReference): Value(model) {

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		var currentValue = context.getThisParameter(constructor)
		if(model.specifier != null) {
			val specifiedTypeDeclaration = model.typeDeclaration
				?: throw CompilerError(model, "Self reference is missing a target type declaration.")
			var currentTypeDeclaration = model.scope.getSurroundingTypeDeclaration()
				?: throw CompilerError(model, "Self reference is missing a surrounding type declaration.")
			while(currentTypeDeclaration != specifiedTypeDeclaration) {
				if(!currentTypeDeclaration.isBound)
					throw CompilerError(model,
						"Specified type declaration of self reference not found in its surrounding type declaration.")
				val parentProperty = constructor.buildGetPropertyPointer(currentTypeDeclaration.unit.llvmType, currentValue,
					Context.PARENT_PROPERTY_INDEX, "_parentProperty")
				currentValue = constructor.buildLoad(constructor.pointerType, parentProperty, "_parent")
				currentTypeDeclaration = currentTypeDeclaration.parent?.scope?.getSurroundingTypeDeclaration()
					?: throw CompilerError(model,
						"Specified type declaration of self reference not found in its surrounding type declaration.")
			}
		}
		return currentValue
	}
}
