package components.code_generation.llvm.models.operations

import components.code_generation.llvm.models.values.VariableValue
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.declarations.TypeAlias
import components.semantic_model.operations.InstanceAccess
import components.semantic_model.types.ObjectType
import errors.internal.CompilerError

class InstanceAccess(override val model: InstanceAccess): VariableValue(model) {

	override fun getLlvmLocation(constructor: LlvmConstructor): LlvmValue {
		val typeDeclaration = (model.providedType as? ObjectType)?.getTypeDeclaration()
		if(typeDeclaration is TypeAlias) {
			val instance = typeDeclaration.instances.find { instance -> instance.name == model.name }
			if(instance != null)
				return instance.unit.llvmLocation
		}
		val objectType = model.effectiveType as? ObjectType
			?: throw CompilerError(model, "Instance access is only allowed on object types.")
		return context.resolveMember(constructor, objectType.getStaticLlvmValue(constructor), model.name, true)
	}
}
