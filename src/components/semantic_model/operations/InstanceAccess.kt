package components.semantic_model.operations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.Instance
import components.semantic_model.declarations.TypeAlias
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.OptionalType
import components.semantic_model.types.Type
import components.semantic_model.values.VariableValue
import errors.internal.CompilerError
import components.syntax_parser.syntax_tree.access.InstanceAccess as InstanceAccessSyntaxTree

class InstanceAccess(override val source: InstanceAccessSyntaxTree, scope: Scope, name: String): VariableValue(source, scope, name) {

	override fun determineTypes() {
		// Do nothing.
		// Type is always inferred.
	}

	override fun setInferredType(inferredType: Type?) {
		super.setInferredType(inferredType)
		val type = providedType ?: return
		val declaration = type.interfaceScope.getValueDeclaration(this)?.declaration as? Instance
			?: throw CompilerError(source, "Inferred type doesn't contain instance value.")
		declaration.usages.add(this)
		this.declaration = declaration
	}

	override fun computeValue(tracker: VariableTracker) {
		super.computeValue(tracker)
		staticValue = declaration?.value
	}

	override fun isAssignableTo(targetType: Type?): Boolean {
		if(targetType is OptionalType)
			return isAssignableTo(targetType.baseType)
		return targetType?.interfaceScope?.hasInstance(name) ?: false
	}

	override fun getLlvmLocation(constructor: LlvmConstructor): LlvmValue {
		val typeDeclaration = (providedType as? ObjectType)?.getTypeDeclaration()
		if(typeDeclaration is TypeAlias) {
			val instance = typeDeclaration.instances.find { instance -> instance.name == name }
			if(instance != null)
				return instance.llvmLocation
		}
		val objectType = effectiveType as? ObjectType
			?: throw CompilerError(source, "Instance access is only allowed on object types.")
		return context.resolveMember(constructor, objectType.getStaticLlvmValue(constructor), name, true)
	}
}
