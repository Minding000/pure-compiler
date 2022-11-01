package components.linting.semantic_model.types

import components.linting.Linter
import components.linting.semantic_model.definitions.OperatorDefinition
import components.linting.semantic_model.definitions.TypeAlias
import components.linting.semantic_model.definitions.TypeDefinition
import components.linting.semantic_model.values.VariableValueDeclaration
import messages.Message
import components.linting.semantic_model.scopes.Scope
import components.parsing.syntax_tree.general.Element
import java.util.LinkedList

class ObjectType(override val source: Element, val name: String, val typeParameters: List<Type> = listOf()):
	Type(source) {
	var definition: TypeDefinition? = null

	constructor(definition: TypeDefinition): this(definition.source, definition.name) {
		this.definition = definition
	}

	constructor(typeParameters: List<Type>, definition: TypeDefinition):
			this(definition.source, definition.name, typeParameters) {
		this.definition = definition
	}

	init {
		units.addAll(typeParameters)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Type {
		val substituteType = typeSubstitutions[definition]
		if(substituteType != null)
			return substituteType
		if(typeParameters.isEmpty())
			return this
		val specificTypeParameters = LinkedList<Type>()
		for(typeParameter in typeParameters)
			specificTypeParameters.add(typeParameter.withTypeSubstitutions(typeSubstitutions))
		val specificType = ObjectType(source, name, specificTypeParameters)
		specificType.definition = definition?.withTypeParameters(specificTypeParameters)
		return specificType
	}

	override fun inferType(genericType: TypeDefinition, sourceType: Type, inferredTypes: MutableSet<Type>) {
		if(sourceType !is ObjectType)
			return
		for(typeParameterIndex in typeParameters.indices) {
			val requiredTypeParameter = typeParameters[typeParameterIndex]
			val sourceTypeParameter = sourceType.typeParameters.getOrNull(typeParameterIndex) ?: break
			requiredTypeParameter.inferType(genericType, sourceTypeParameter, inferredTypes)
		}
		if(definition == genericType)
			inferredTypes.add(sourceType)
	}

	override fun onNewType(type: TypeDefinition) {
		this.scope.addType(type)
	}

	override fun onNewValue(value: VariableValueDeclaration) {
		this.scope.addValue(value)
	}

	override fun onNewOperator(operator: OperatorDefinition) {
		this.scope.addOperator(operator)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, scope)
		if(definition == null) {
			definition = scope.resolveType(name)
			if(definition == null)
				linter.addMessage(source, "Type '$name' hasn't been declared yet.", Message.Type.ERROR)
		}
	}

	override fun resolveGenerics(linter: Linter) {
		super.resolveGenerics(linter)
		if(typeParameters.isNotEmpty())
			definition = definition?.withTypeParameters(typeParameters)
		definition?.scope?.subscribe(this)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		(definition?.baseDefinition ?: definition)?.let { definition ->
			val genericTypes = definition.scope.getGenericTypeDefinitions()
			if(typeParameters.size != genericTypes.size) {
				linter.addMessage(source, "Number of provided type parameters " +
					"(${typeParameters.size}) doesn't match number of declared " +
					"generic types (${genericTypes.size}).", Message.Type.ERROR) //TODO write test for this
			}
			if(typeParameters.isEmpty())
				return
			for(parameterIndex in genericTypes.indices) {
				val genericType = genericTypes[parameterIndex]
				val typeParameter = typeParameters.getOrNull(parameterIndex) ?: break
				if(!genericType.acceptsSubstituteType(typeParameter)) {
					linter.addMessage(source, "The type parameter " +
						"'$typeParameter' is not assignable to '$genericType'.", Message.Type.ERROR)
				}
			}
		}
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		(definition as? TypeAlias)?.let { typeAlias ->
			return unresolvedSourceType.isAssignableTo(typeAlias.referenceType)
		}
		if(unresolvedSourceType is StaticType || unresolvedSourceType is FunctionType)
			return false
		return unresolvedSourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = resolveTypeAlias(unresolvedTargetType)
		(definition as? TypeAlias)?.let { typeAlias ->
			return typeAlias.referenceType.isAssignableTo(targetType)
		}
		if(targetType is StaticType || targetType is FunctionType)
			return false
		if(targetType !is ObjectType)
			return targetType.accepts(this)
		if(equals(targetType))
			return true
		return definition?.superType?.isAssignableTo(targetType) ?: false
	}

	override fun getKeyType(linter: Linter): Type? { //TODO write test for this
		if(typeParameters.size != 2) {
			linter.addMessage("Type '$this' doesn't have a key type.", Message.Type.ERROR)
			return null
		}
		return typeParameters.first()
	}

	override fun getValueType(linter: Linter): Type? { //TODO write test for this
		if(!(typeParameters.size == 1 || typeParameters.size == 2)) {
			linter.addMessage("Type '$this' doesn't have a value type.", Message.Type.ERROR)
			return null
		}
		return typeParameters.last()
	}

	override fun equals(other: Any?): Boolean {
		if(other !is Type)
			return false
		val otherType = resolveTypeAlias(other)
		(definition as? TypeAlias)?.let { typeAlias ->
			return typeAlias.referenceType == otherType
		}
		if(otherType !is ObjectType)
			return false
		if(definition != otherType.definition)
			return false
		if(typeParameters.size != otherType.typeParameters.size)
			return false
		for(genericParameterIndex in typeParameters.indices)
			if(typeParameters[genericParameterIndex] != otherType.typeParameters[genericParameterIndex])
				return false
		return true
	}

	override fun hashCode(): Int {
		var result = typeParameters.hashCode()
		result = 31 * result + (definition?.hashCode() ?: 0)
		return result
	}

	override fun toString(): String {
		if(typeParameters.isEmpty())
			return name
		return typeParameters.joinToString(", ", "<", ">$name")
	}
}
