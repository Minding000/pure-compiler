package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.general.File
import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.models.values.FunctionObject
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.scopes.FileScope
import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.StaticType
import errors.internal.CompilerError
import org.bytedeco.llvm.LLVM.LLVMValueRef
import java.util.*
import components.semantic_model.general.File as SemanticFileModel

abstract class ValueDeclaration(override val model: ValueDeclaration, val value: Value? = null, units: List<Unit> = emptyList()):
	Unit(model, listOfNotNull(value, *units.toTypedArray())) {
	lateinit var llvmLocation: LLVMValueRef

	override fun declare(constructor: LlvmConstructor) {
		super.declare(constructor)
		if(model.providedType is StaticType)
			return
		if(model.scope is FileScope)
			llvmLocation = constructor.declareGlobal("${model.name}_Global", model.effectiveType?.getLlvmType(constructor))
	}

	override fun compile(constructor: LlvmConstructor) {
		val value = value
		if(model.scope is TypeScope || model.providedType is StaticType) {
			if(value is FunctionObject)
				value.compile(constructor)
			return
		}
		if(model.scope is FileScope) {
			constructor.defineGlobal(llvmLocation, constructor.nullPointer)
		} else {
			llvmLocation = constructor.buildStackAllocation(model.effectiveType?.getLlvmType(constructor), "${model.name}_Variable")
		}
		if(value != null) {
			val llvmValue = ValueConverter.convertIfRequired(model, constructor, value.getLlvmValue(constructor), value.model.effectiveType,
				value.model.hasGenericType, model.effectiveType, false, model.conversion)
			constructor.buildStore(llvmValue, llvmLocation)
		}
	}

	override fun determineFileInitializationOrder(filesToInitialize: LinkedList<File>) {
		if(hasDeterminedFileInitializationOrder)
			return
		super.determineFileInitializationOrder(filesToInitialize)
		val file = getSurrounding<File>() ?: throw CompilerError(model, "Value declaration outside of file.")
		if(requiresFileRunner())
			file.requiresFileRunner = true
		file.determineFileInitializationOrder(filesToInitialize)
	}

	open fun requiresFileRunner(): Boolean {
		//TODO what about nested bound enums and objects?
		// - enums are partly handled by instances (but isBound is not taken into consideration)
		return model.parent is SemanticFileModel
	}
}
