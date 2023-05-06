package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.*
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.definition.TypeParameterCountMismatch
import logger.issues.definition.TypeParameterNotAssignable
import logger.issues.resolution.NotFound

open class ObjectType(override val source: Element, scope: Scope, val enclosingType: ObjectType?, val typeParameters: List<Type>,
					  val name: String, var definition: TypeDefinition? = null): Type(source, scope) {
	private var isInSpecificContext = true

	constructor(source: Element, surroundingScope: Scope, name: String): this(source, surroundingScope, null, listOf(), name)

	constructor(definition: TypeDefinition):
		this(definition.source, definition.scope, null, listOf(), definition.name, definition)

	constructor(typeParameters: List<Type>, definition: TypeDefinition):
		this(definition.source, definition.scope, null, typeParameters, definition.name, definition)

	init {
		addUnits(enclosingType)
		addUnits(typeParameters)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): Type {
		val substituteType = typeSubstitutions[definition]
		if(substituteType != null)
			return substituteType
		val isBound = definition?.isBound == true
		if(!isBound && typeParameters.isEmpty())
			return this
		val specificTypeParameters = typeParameters.map { typeParameter ->
			typeParameter.withTypeSubstitutions(linter, typeSubstitutions) }
		//TODO this might be nicer if it was written with a return in the callback
		// -> withTypeParameter needs to have inline modifier
		val specificType = ObjectType(source, scope, enclosingType, specificTypeParameters, name)
		val typeSubstitutions = if(isBound) typeSubstitutions else mapOf()
		definition?.withTypeParameters(linter, specificTypeParameters, typeSubstitutions) { specificDefinition ->
			specificType.definition = specificDefinition
			specificDefinition.scope.subscribe(specificType)
		}
		return specificType
	}

	override fun simplified(): ObjectType {
		return ObjectType(source, scope, enclosingType?.simplified(), typeParameters.map(Type::simplified), name, definition)
	}

	fun setIsNonSpecificContext() {
		isInSpecificContext = false
		enclosingType?.setIsNonSpecificContext()
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

	override fun onNewInitializer(initializer: InitializerDefinition) {
		interfaceScope.addInitializer(initializer)
	}

	override fun resolveDefinitions(linter: Linter) {
		super.resolveDefinitions(linter)
		if(definition == null) {
			val sourceScope = enclosingType?.interfaceScope ?: scope
			definition = sourceScope.resolveType(name)
			if(definition == null)
				linter.addIssue(NotFound(source, "Type", name))
			if(definition?.isBound != true)
				enclosingType?.setIsNonSpecificContext()
		}
		if(typeParameters.isNotEmpty()) {
			definition?.withTypeParameters(linter, typeParameters) { specificDefinition ->
				definition = specificDefinition
			}
		}
		//TODO ask the definition to resolve all its member signatures
		definition?.scope?.subscribe(this)
		val definition = definition
		if(definition is TypeAlias) {
			isAlias = true
			effectiveType = definition.getEffectiveType(linter)
		}
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(isInSpecificContext)
			ensureSpecificDefinition(linter)
	}

	private fun ensureSpecificDefinition(linter: Linter) {
		(definition?.baseDefinition ?: definition)?.let { definition ->
			val genericTypes = definition.scope.getGenericTypeDefinitions()
			if(typeParameters.size != genericTypes.size)
				linter.addIssue(TypeParameterCountMismatch(source, typeParameters, genericTypes))
			if(typeParameters.isEmpty())
				return
			for(parameterIndex in genericTypes.indices) {
				val genericType = genericTypes[parameterIndex]
				val typeParameter = typeParameters.getOrNull(parameterIndex) ?: break
				if(!genericType.acceptsSubstituteType(typeParameter))
					linter.addIssue(TypeParameterNotAssignable(source, typeParameter, genericType))
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
		return definition?.scope?.getAbstractMembers() ?: listOf()
	}

	override fun getPropertiesToBeInitialized(): List<PropertyDeclaration> {
		return definition?.scope?.getPropertiesToBeInitialized() ?: listOf()
	}

	override fun getConversionsFrom(linter: Linter, sourceType: Type): List<InitializerDefinition> {
		return definition?.getConversionsFrom(linter, sourceType) ?: listOf()
	}

	override fun equals(other: Any?): Boolean {
		if(other !is Type)
			return false
		if(isAlias || other.isAlias)
			return effectiveType == other.effectiveType
		if(other !is ObjectType)
			return false
		if(definition != other.definition)
			return false
		if(definition == null && name != other.name)
			return false
		if(typeParameters.size != other.typeParameters.size)
			return false
		for(genericParameterIndex in typeParameters.indices)
			if(typeParameters[genericParameterIndex] != other.typeParameters[genericParameterIndex])
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
