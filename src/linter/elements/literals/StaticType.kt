package linter.elements.literals

import linter.elements.definitions.InitializerDefinition
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration

class StaticType(val definition: TypeDefinition): Type() {

	init {
		definition.scope.subscribe(this)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<Type, Type>): StaticType {
		return this
	}

	override fun onNewType(type: TypeDefinition) {
		this.scope.addType(type)
	}

	override fun onNewValue(value: VariableValueDeclaration) {
		if(value.isConstant)
			this.scope.addValue(value)
	}

	override fun onNewInitializer(initializer: InitializerDefinition) {
		this.scope.addInitializer(initializer)
	}

	override fun accepts(sourceType: Type): Boolean {
		return sourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(targetType: Type): Boolean {
		if(targetType is FunctionType)
			return false
		if(targetType !is StaticType)
			return targetType.accepts(this)
		if(equals(targetType))
			return true
		return definition.superType?.isAssignableTo(targetType) ?: false
	}

	override fun equals(other: Any?): Boolean {
		if(other !is StaticType)
			return false
		if(definition != other.definition)
			return false
		return true
	}

	override fun hashCode(): Int {
		return definition.hashCode()
	}

	override fun toString(): String {
		return definition.name
	}
}