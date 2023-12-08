package components.semantic_model.types

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.*
import components.semantic_model.scopes.Scope
import components.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.declaration.TypeParameterCountMismatch
import logger.issues.declaration.TypeParameterNotAssignable
import logger.issues.resolution.NotFound
import java.util.*

open class ObjectType(override val source: SyntaxTreeNode, scope: Scope, var enclosingType: ObjectType?, val typeParameters: List<Type>,
					  val name: String, typeDeclaration: TypeDeclaration? = null, val isSpecific: Boolean = false): Type(source, scope) {
	private var isInSpecificContext = true
	protected var typeDeclarationCache: TypeDeclaration? = typeDeclaration

	constructor(source: SyntaxTreeNode, surroundingScope: Scope, name: String): this(source, surroundingScope, null,
		emptyList(), name)

	constructor(typeDeclaration: TypeDeclaration):
		this(typeDeclaration.source, typeDeclaration.scope, null, emptyList(), typeDeclaration.name, typeDeclaration)

	constructor(typeParameters: List<Type>, typeDeclaration: TypeDeclaration):
		this(typeDeclaration.source, typeDeclaration.scope, null, typeParameters, typeDeclaration.name, typeDeclaration)

	init {
		addSemanticModels(enclosingType)
		addSemanticModels(typeParameters)
	}

	override fun createCopyWithTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): Type {
		var typeDeclaration = getTypeDeclaration()
		if(typeDeclaration is WhereClauseCondition)
			typeDeclaration = typeDeclaration.getSubjectTypeDefinition()
		val substituteType = typeSubstitutions[typeDeclaration]
		if(substituteType != null)
			return substituteType
		if(enclosingType == null && typeParameters.isEmpty())
			return this
		val specificTypeParameters = typeParameters.map { typeParameter -> typeParameter.withTypeSubstitutions(typeSubstitutions) }
		return ObjectType(source, scope, enclosingType?.withTypeSubstitutions(typeSubstitutions) as? ObjectType, specificTypeParameters,
			name, typeDeclaration)
	}

	override fun simplified(): ObjectType {
		return ObjectType(source, scope, enclosingType?.simplified(), typeParameters.map(Type::simplified), name, getTypeDeclaration())
	}

	override fun isMemberAccessible(signature: FunctionSignature, requireSpecificType: Boolean): Boolean {
		val typeDeclaration = getTypeDeclaration() ?: return false
		if(requireSpecificType && !isSpecific) {
			if(typeDeclaration is GenericTypeDeclaration || typeDeclaration is WhereClauseCondition || typeDeclaration is TypeAlias)
				return typeDeclaration.superType?.isMemberAccessible(signature, true) == true
		} else {
			if(typeDeclaration == signature.original.parentDefinition)
				return true
			if(typeDeclaration is GenericTypeDeclaration || typeDeclaration is WhereClauseCondition || typeDeclaration is TypeAlias)
				return typeDeclaration.superType?.isMemberAccessible(signature) == true
		}
		return false
	}

	fun setIsNonSpecificContext() {
		isInSpecificContext = false
		enclosingType?.setIsNonSpecificContext()
	}

	override fun inferTypeParameter(typeParameter: TypeDeclaration, sourceType: Type, inferredTypes: MutableList<Type>) {
		if(sourceType is ObjectType) {
			for(typeParameterIndex in typeParameters.indices) {
				val requiredTypeParameter = typeParameters[typeParameterIndex]
				val sourceTypeParameter = sourceType.typeParameters.getOrNull(typeParameterIndex) ?: break
				requiredTypeParameter.inferTypeParameter(typeParameter, sourceTypeParameter, inferredTypes)
			}
		}
		if(getTypeDeclaration() == typeParameter)
			inferredTypes.add(sourceType)
	}

	override fun getInitializers(): List<InitializerDefinition> {
		return getTypeDeclaration()?.scope?.initializers ?: emptyList()
	}

	override fun getAllInitializers(): List<InitializerDefinition> {
		return getTypeDeclaration()?.getAllInitializers() ?: emptyList()
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		return getTypeDeclaration()?.scope?.getDirectTypeDeclaration(name)
	}

	override fun getValueDeclaration(name: String): Triple<ValueDeclaration?, List<WhereClauseCondition>?, Type?> {
		val (valueDeclaration, whereClauseConditions, type) = getTypeDeclaration()?.scope?.getDirectValueDeclaration(name)
			?: return Triple(null, null, null)
		val typeSubstitutions = getTypeSubstitutions()
		val specificType = type?.withTypeSubstitutions(typeSubstitutions)
		if(whereClauseConditions == null)
			return Triple(valueDeclaration, null, specificType)
		val specificWhereClauseConditions = LinkedList<WhereClauseCondition>()
		for(whereClauseCondition in whereClauseConditions)
			specificWhereClauseConditions.add(whereClauseCondition.withTypeSubstitutions(typeSubstitutions))
		return Triple(valueDeclaration, specificWhereClauseConditions, specificType)
	}

	private fun getTypeSubstitutions(): Map<TypeDeclaration, Type> {
		val typeDeclaration = getTypeDeclaration() ?: return emptyMap()
		val genericTypeDeclarations = typeDeclaration.scope.getGenericTypeDeclarations()
		val directTypeSubstitutions = HashMap<TypeDeclaration, Type>()
		for(genericTypeDeclarationIndex in genericTypeDeclarations.indices) {
			val genericTypeDeclaration = genericTypeDeclarations[genericTypeDeclarationIndex]
			val substituteType = typeParameters.getOrNull(genericTypeDeclarationIndex) ?: continue
			directTypeSubstitutions[genericTypeDeclaration] = substituteType
		}
		if(!typeDeclaration.isBound)
			return directTypeSubstitutions
		val enclosingTypeSubstitutions = enclosingType?.getTypeSubstitutions() ?: return directTypeSubstitutions
		val allGenericTypeDeclarations = directTypeSubstitutions.toMutableMap()
		allGenericTypeDeclarations.putAll(enclosingTypeSubstitutions)
		return allGenericTypeDeclarations
	}

	fun getTypeDeclaration(): TypeDeclaration? {
		determineTypes()
		return typeDeclarationCache
	}

	override fun resolveTypeDeclarations() {
		super.resolveTypeDeclarations()
		if(typeDeclarationCache == null) {
			val sourceScope = enclosingType?.interfaceScope ?: scope
			typeDeclarationCache = sourceScope.getTypeDeclaration(name)
			if(typeDeclarationCache == null)
				context.addIssue(NotFound(source, "Type", name))
			inferEnclosingType()
		}
		val typeDeclaration = typeDeclarationCache
		if(typeDeclaration is TypeAlias)
			effectiveType = typeDeclaration.getEffectiveType()
	}

	private fun inferEnclosingType() {
		val typeDeclaration = getTypeDeclaration()
		val parentTypeDeclaration = typeDeclaration?.parentTypeDeclaration
		if(parentTypeDeclaration != null && enclosingType == null)
			enclosingType = ObjectType(parentTypeDeclaration.getGenericTypes(), parentTypeDeclaration)
		enclosingType?.inferEnclosingType()
		if(typeDeclaration?.isBound != true)
			enclosingType?.setIsNonSpecificContext()
	}

	override fun validate() {
		super.validate()
		if(isInSpecificContext)
			validateTypeParameters()
	}

	private fun validateTypeParameters() {
		val typeDeclaration = getTypeDeclaration() ?: return
		val genericTypes = typeDeclaration.scope.getGenericTypeDeclarations()
		if(typeParameters.size != genericTypes.size)
			context.addIssue(TypeParameterCountMismatch(source, typeParameters, genericTypes))
		if(typeParameters.isEmpty())
			return
		for(parameterIndex in genericTypes.indices) {
			val genericType = genericTypes[parameterIndex]
			val typeParameter = typeParameters.getOrNull(parameterIndex) ?: break
			if(!genericType.acceptsSubstituteType(typeParameter))
				context.addIssue(TypeParameterNotAssignable(source, typeParameter, genericType))
		}
	}

	override fun isInstanceOf(specialType: SpecialType): Boolean {
		if(specialType.matches(this))
			return true
		return getTypeDeclaration()?.getLinkedSuperType()?.isInstanceOf(specialType) ?: false
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		if(unresolvedSourceType is StaticType)
			return false
		return unresolvedSourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
		val typeDeclaration = getTypeDeclaration()
		if(typeDeclaration is TypeAlias)
			return effectiveType.isAssignableTo(targetType)
		if(targetType is StaticType || targetType is FunctionType)
			return false
		if(targetType !is ObjectType)
			return targetType.accepts(this)
		if(equals(targetType))
			return true
		return typeDeclaration?.getLinkedSuperType()?.isAssignableTo(targetType) ?: false
	}

	override fun getPotentiallyUnimplementedAbstractMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> {
		val typeDeclaration = getTypeDeclaration() ?: return emptyList()
		val abstractMemberDeclarations = typeDeclaration.getUnimplementedAbstractSuperMemberDeclarations().toMutableList()
		val typeSubstitutions = getTypeSubstitutions()
		abstractMemberDeclarations.addAll(typeDeclaration.scope.getAbstractMemberDeclarations().map { abstractMemberDeclaration ->
			Pair(abstractMemberDeclaration, typeSubstitutions) })
		return abstractMemberDeclarations
	}

	override fun implements(abstractMember: MemberDeclaration, typeSubstitutions: Map<TypeDeclaration, Type>): Boolean {
		val typeDeclaration = getTypeDeclaration() ?: return false
		return typeDeclaration.implements(abstractMember, typeSubstitutions)
	}

	override fun getSpecificMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> {
		val typeDeclaration = getTypeDeclaration() ?: return emptyList()
		val typeSubstitutions = getTypeSubstitutions()
		return typeDeclaration.scope.getSpecificMemberDeclarations().map { abstractMemberDeclaration ->
			Pair(abstractMemberDeclaration, typeSubstitutions) }
	}

	override fun getPropertiesToBeInitialized(): List<PropertyDeclaration> {
		return getTypeDeclaration()?.scope?.getPropertiesToBeInitialized() ?: emptyList()
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return getTypeDeclaration()?.getConversionsFrom(sourceType) ?: emptyList()
	}

	override fun equals(other: Any?): Boolean {
		if(other !is Type)
			return false
		val otherType = other.effectiveType
		val typeDeclaration = getTypeDeclaration()
		if(typeDeclaration is TypeAlias)
			return effectiveType == otherType
		if(otherType !is ObjectType)
			return false
		if(typeDeclaration != otherType.getTypeDeclaration())
			return false
		if(typeDeclaration == null && name != otherType.name)
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
		result = 31 * result + (getTypeDeclaration()?.hashCode() ?: 0)
		return result
	}

	override fun toString(): String {
		var stringRepresentation = ""
		if(isSpecific)
			stringRepresentation += "specific "
		if(enclosingType != null && getTypeDeclaration() !is GenericTypeDeclaration)
			stringRepresentation += "$enclosingType."
		stringRepresentation += if(typeParameters.isEmpty())
			name
		else
			typeParameters.joinToString(", ", "<", ">$name")
		return stringRepresentation
	}

	override fun createLlvmType(constructor: LlvmConstructor): LlvmType {
		if(SpecialType.FLOAT.matches(this))
			return constructor.floatType
		if(SpecialType.INTEGER.matches(this))
			return constructor.i32Type
		if(SpecialType.BYTE.matches(this))
			return constructor.byteType
		if(SpecialType.BOOLEAN.matches(this))
			return constructor.booleanType
		if(SpecialType.NOTHING.matches(this))
			return constructor.voidType
		if(SpecialType.NEVER.matches(this))
			return constructor.voidType
		return constructor.pointerType
	}
}
