package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.MemberDeclaration
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeAlias
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.syntax_parser.syntax_tree.general.Element
import messages.Message
import java.util.*

open class ObjectType(override val source: Element, scope: Scope, val enclosingType: ObjectType?, val typeParameters: List<Type>,
					  val name: String, var definition: TypeDefinition? = null): Type(source, scope) {

	constructor(source: Element, surroundingScope: Scope, name: String): this(source, surroundingScope, null, listOf(), name)

	constructor(definition: TypeDefinition):
		this(definition.source, definition.scope, null, listOf(), definition.name, definition)

	constructor(typeParameters: List<Type>, definition: TypeDefinition):
		this(definition.source, definition.scope, null, typeParameters, definition.name, definition)

	init {
		addUnits(enclosingType)
		addUnits(typeParameters)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Type {
		val substituteType = typeSubstitutions[definition]
		if(substituteType != null)
			return substituteType
		if(typeParameters.isEmpty())
			return this
		val specificTypeParameters = typeParameters.map { typeParameter ->
			typeParameter.withTypeSubstitutions(typeSubstitutions) }
		//TODO this might be nicer if it was written with a return in the callback
		// -> withTypeParameter needs to have inline modifier
		val specificType = ObjectType(source, scope, enclosingType, specificTypeParameters, name)
		definition?.withTypeParameters(specificTypeParameters) { specificDefinition ->
			specificType.definition = specificDefinition
		}
		return specificType
	}

	override fun simplified(): ObjectType {
		return ObjectType(source, scope, enclosingType?.simplified(), typeParameters.map(Type::simplified), name, definition)
	}

	override fun inferType(genericType: TypeDefinition, sourceType: Type, inferredTypes: MutableList<Type>) {
		if(sourceType is ObjectType) {
			for(typeParameterIndex in typeParameters.indices) {
				val requiredTypeParameter = typeParameters[typeParameterIndex]
				val sourceTypeParameter = sourceType.typeParameters.getOrNull(typeParameterIndex) ?: break
				requiredTypeParameter.inferType(genericType, sourceTypeParameter, inferredTypes)
			}
		}
		if(definition == genericType)
			inferredTypes.add(sourceType)
	}

	override fun onNewType(type: TypeDefinition) {
		interfaceScope.addType(type)
	}

	override fun onNewValue(value: InterfaceMember) {
		interfaceScope.addValue(value)
	}

	override fun linkTypes(linter: Linter) {
		super.linkTypes(linter)
		if(definition == null) {
			enclosingType?.resolveGenerics(linter)
			val sourceScope = enclosingType?.interfaceScope ?: scope
			definition = sourceScope.resolveType(name)
			if(definition == null)
				linter.addMessage(source, "Type '$name' hasn't been declared yet.", Message.Type.ERROR)
		}
	}

	override fun resolveGenerics(linter: Linter) {
		for(unit in units)
			if(unit !== enclosingType)
				unit.resolveGenerics(linter)
		if(typeParameters.isNotEmpty()) {
			definition?.withTypeParameters(typeParameters) { specificDefinition ->
				definition = specificDefinition
			}
		}
		definition?.scope?.subscribe(this)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		(definition?.baseDefinition ?: definition)?.let { definition ->
			val genericTypes = definition.scope.getGenericTypeDefinitions()
			if(typeParameters.size != genericTypes.size) {
				linter.addMessage(source, "Number of provided type parameters " +
					"(${typeParameters.size}) doesn't match number of declared " +
					"generic types (${genericTypes.size}).", Message.Type.ERROR)
			}
			if(typeParameters.isEmpty())
				return
			for(parameterIndex in genericTypes.indices) {
				val genericType = genericTypes[parameterIndex]
				val typeParameter = typeParameters.getOrNull(parameterIndex) ?: break
				if(!genericType.acceptsSubstituteType(typeParameter)) {
					linter.addMessage(source, "The type parameter '$typeParameter' is not assignable to '$genericType'.",
						Message.Type.ERROR)
				}
			}
		}
	}

	override fun isInstanceOf(type: Linter.SpecialType): Boolean {
		if(type.matches(this))
			return true
		return definition?.superType?.isInstanceOf(type) ?: false
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		(definition as? TypeAlias)?.let { typeAlias ->
			return unresolvedSourceType.isAssignableTo(typeAlias.referenceType)
		}
		if(unresolvedSourceType is StaticType)
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

	override fun getAbstractMembers(): List<MemberDeclaration> {
		return definition?.scope?.getAbstractMembers() ?: LinkedList()
	}

	override fun getPropertiesToBeInitialized(): List<PropertyDeclaration> {
		return definition?.scope?.getPropertiesToBeInitialized() ?: LinkedList()
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
		var stringRepresentation = ""
		if(enclosingType != null)
			stringRepresentation += "$enclosingType."
		stringRepresentation += if(typeParameters.isEmpty())
			name
		else
			typeParameters.joinToString(", ", "<", ">$name")
		return stringRepresentation
	}
}
