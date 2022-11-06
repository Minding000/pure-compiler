package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.VariableValueDeclaration

class StaticType(val definition: TypeDefinition): Type(definition.source) {

	init {
		definition.scope.subscribe(this)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): StaticType {
		// StaticTypes don't have the recursion issues ObjectTypes have,
		//  since there can't be a StaticType inside a class definition
		lateinit var specificType: StaticType
		definition.withTypeSubstitutions(typeSubstitutions) { specificDefinition ->
			specificType = StaticType(specificDefinition)
		}
		return specificType
	}

	fun withTypeParameters(typeParameters: List<Type>, onCompletion: (StaticType) -> Unit) {
		definition.withTypeParameters(typeParameters) { specificDefinition ->
			onCompletion(StaticType(specificDefinition))
		}
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

	override fun accepts(unresolvedSourceType: Type): Boolean {
		return unresolvedSourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = resolveTypeAlias(unresolvedTargetType)
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
