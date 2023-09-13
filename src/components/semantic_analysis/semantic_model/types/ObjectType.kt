package components.semantic_analysis.semantic_model.types

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.declarations.*
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.declaration.TypeParameterCountMismatch
import logger.issues.declaration.TypeParameterNotAssignable
import logger.issues.resolution.NotFound
import java.util.*

open class ObjectType(override val source: SyntaxTreeNode, scope: Scope, var enclosingType: ObjectType?, val typeParameters: List<Type>,
					  val name: String, var typeDeclaration: TypeDeclaration? = null): Type(source, scope) {
	private var isInSpecificContext = true

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
		return ObjectType(source, scope, enclosingType?.simplified(), typeParameters.map(Type::simplified), name, typeDeclaration)
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
		if(typeDeclaration == typeParameter)
			inferredTypes.add(sourceType)
	}

	override fun onNewTypeDeclaration(newTypeDeclaration: TypeDeclaration) {
		interfaceScope.addTypeDeclaration(newTypeDeclaration)
	}

	override fun onNewInterfaceMember(newInterfaceMember: InterfaceMember) {
		interfaceScope.addInterfaceMember(newInterfaceMember)
	}

	override fun onNewInitializer(newInitializer: InitializerDefinition) {
		interfaceScope.addInitializer(newInitializer)
	}

	override fun getValueDeclaration(name: String): Pair<ValueDeclaration?, Type?> {
		val (valueDeclaration, type) = typeDeclaration?.scope?.getValueDeclaration(name) ?: return Pair(null, null)
		val typeSubstitutions = getTypeSubstitutions()
		return Pair(valueDeclaration, type?.withTypeSubstitutions(typeSubstitutions))
	}

	private fun getTypeSubstitutions(): Map<TypeDeclaration, Type> {
		val typeDeclaration = typeDeclaration ?: return emptyMap()
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

	override fun resolveTypeDeclarations() {
		super.resolveTypeDeclarations()
		if(typeDeclaration == null) {
			val sourceScope = enclosingType?.interfaceScope ?: scope
			typeDeclaration = sourceScope.getTypeDeclaration(name)
			if(typeDeclaration == null)
				context.addIssue(NotFound(source, "Type", name))
			inferEnclosingType()
		}
		typeDeclaration?.scope?.addSubscriber(this)
		val typeDeclaration = typeDeclaration
		if(typeDeclaration is TypeAlias)
			effectiveType = typeDeclaration.getEffectiveType()
	}

	private fun inferEnclosingType() {
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
		val typeDeclaration = typeDeclaration ?: return
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
		return typeDeclaration?.getLinkedSuperType()?.isInstanceOf(specialType) ?: false
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		if(unresolvedSourceType is StaticType)
			return false
		return unresolvedSourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
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

	override fun getAbstractMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> {
		val typeDeclaration = typeDeclaration ?: return emptyList()
		val abstractMemberDeclarations = LinkedList<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>>()
		val superScope = typeDeclaration.scope.superScope
		if(superScope != null)
			abstractMemberDeclarations.addAll(superScope.getAbstractMemberDeclarations())
		val typeSubstitutions = getTypeSubstitutions()
		abstractMemberDeclarations.addAll(typeDeclaration.scope.getAbstractMemberDeclarations().map { abstractMemberDeclaration ->
			Pair(abstractMemberDeclaration, typeSubstitutions) })
		return abstractMemberDeclarations
	}

	override fun getPropertiesToBeInitialized(): List<PropertyDeclaration> {
		return typeDeclaration?.scope?.getPropertiesToBeInitialized() ?: emptyList()
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return typeDeclaration?.getConversionsFrom(sourceType) ?: emptyList()
	}

	override fun equals(other: Any?): Boolean {
		if(other !is Type)
			return false
		val otherType = other.effectiveType
		if(typeDeclaration is TypeAlias)
			return effectiveType == otherType
		if(otherType !is ObjectType)
			return false
		if(typeDeclaration != otherType.typeDeclaration)
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
		result = 31 * result + (typeDeclaration?.hashCode() ?: 0)
		return result
	}

	override fun toString(): String {
		var stringRepresentation = ""
		if(enclosingType != null && typeDeclaration !is GenericTypeDeclaration)
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
		if(SpecialType.BOOLEAN.matches(this))
			return constructor.booleanType
		if(SpecialType.NOTHING.matches(this))
			return constructor.voidType
		if(SpecialType.NEVER.matches(this))
			return constructor.voidType
		return constructor.pointerType
	}
}
