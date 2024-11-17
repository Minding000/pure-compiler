package components.code_generation.llvm.models.values

import components.code_generation.llvm.models.general.File
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.declarations.*
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.semantic_model.values.VariableValue
import errors.internal.CompilerError
import java.util.*

//TODO rename to VariableUsage / Reference
open class VariableValue(override val model: VariableValue): Value(model) {
	var whereClauseConditions: List<WhereClauseCondition>? = null
	protected open var staticType: Type? = null

	override fun getLlvmLocation(constructor: LlvmConstructor): LlvmValue? {
		val declaration = model.declaration
		if(declaration is ComputedPropertyDeclaration)
			throw CompilerError(model, "Computed properties do not have a location.")
		return if(declaration is PropertyDeclaration) {
			var currentValue = context.getThisParameter(constructor)
			var currentTypeDeclaration = model.scope.getSurroundingTypeDeclaration()
				?: throw CompilerError(model, "Property is referenced by variable value outside of a type declaration.")
			while(!isDeclaredIn(declaration, currentTypeDeclaration)) {
				if(!currentTypeDeclaration.isBound)
					throw CompilerError(model,
						"Type declaration of property referenced by variable value not found in its surrounding type declaration.")
				val parentProperty = constructor.buildGetPropertyPointer(currentTypeDeclaration.unit.llvmType, currentValue,
					Context.PARENT_PROPERTY_INDEX, "_parentProperty")
				currentValue = constructor.buildLoad(constructor.pointerType, parentProperty, "_parent")
				currentTypeDeclaration = currentTypeDeclaration.parentTypeDeclaration
					?: throw CompilerError(model,
						"Type declaration of property referenced by variable value not found in its surrounding type declaration.")
			}
			context.resolveMember(constructor, currentValue, model.name, (declaration as? InterfaceMember)?.isStatic ?: false)
		} else {
			declaration?.unit?.llvmLocation
		}
	}

	private fun isDeclaredIn(property: PropertyDeclaration, typeDeclaration: TypeDeclaration): Boolean {
		if(property.parentTypeDeclaration == typeDeclaration)
			return true
		for(superType in typeDeclaration.getDirectSuperTypes()) {
			if(isDeclaredIn(property, superType.getTypeDeclaration() ?: continue))
				return true
		}
		return false
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val declaration = model.declaration
		if(declaration?.providedType is StaticType)
			return declaration.unit.llvmLocation
		if(declaration is ComputedPropertyDeclaration) {
			val setStatement = declaration.setter
			if(setStatement != null && model.isIn(setStatement))
				return constructor.getLastParameter()
			return buildGetterCall(constructor, declaration)
		}
		val llvmType = if(model.hasGenericType) constructor.pointerType else model.effectiveType?.getLlvmType(constructor)
		return constructor.buildLoad(llvmType, getLlvmLocation(constructor), model.name)
	}

	private fun buildGetterCall(constructor: LlvmConstructor, computedPropertyDeclaration: ComputedPropertyDeclaration): LlvmValue {
		val exceptionAddress = context.getExceptionParameter(constructor)
		val targetValue = context.getThisParameter(constructor)
		val functionAddress = context.resolveFunction(constructor, targetValue, computedPropertyDeclaration.getterIdentifier)
		val returnValue = constructor.buildFunctionCall(computedPropertyDeclaration.computedPropertyUnit.llvmGetterType, functionAddress,
			listOf(exceptionAddress, targetValue), "_computedPropertyGetterResult")
		context.continueRaise(constructor, model)
		return returnValue
	}

	override fun determineFileInitializationOrder(filesToInitialize: LinkedList<File>) {
		if(hasDeterminedFileInitializationOrder)
			return
		super.determineFileInitializationOrder(filesToInitialize)
		model.declaration?.unit?.determineFileInitializationOrder(filesToInitialize)
	}
}
