package components.semantic_analysis.semantic_model.types

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.definitions.*
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.definition.TypeParameterCountMismatch
import logger.issues.definition.TypeParameterNotAssignable
import logger.issues.resolution.NotFound

open class ObjectType(override val source: SyntaxTreeNode, scope: Scope, val enclosingType: ObjectType?, val typeParameters: List<Type>,
					  val name: String, var typeDeclaration: TypeDeclaration? = null): Type(source, scope) {
	private var isInSpecificContext = true

	constructor(source: SyntaxTreeNode, surroundingScope: Scope, name: String): this(source, surroundingScope, null,
		emptyList(), name)

	constructor(typeDeclaration: TypeDeclaration):
		this(typeDeclaration.source, typeDeclaration.scope, null, listOf(), typeDeclaration.name, typeDeclaration)

	constructor(typeParameters: List<Type>, typeDeclaration: TypeDeclaration):
		this(typeDeclaration.source, typeDeclaration.scope, null, typeParameters, typeDeclaration.name, typeDeclaration)

	init {
		addSemanticModels(enclosingType)
		addSemanticModels(typeParameters)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): Type {
		val substituteType = typeSubstitutions[typeDeclaration]
		if(substituteType != null)
			return substituteType
		val isBound = typeDeclaration?.isBound == true
		if(!isBound && typeParameters.isEmpty())
			return this
		val specificTypeParameters = typeParameters.map { typeParameter -> typeParameter.withTypeSubstitutions(typeSubstitutions) }
		//TODO this might be nicer if it was written with a return in the callback
		// -> withTypeParameter needs to have inline modifier
		val specificType = ObjectType(source, scope, enclosingType, specificTypeParameters, name)
		val effectiveTypeSubstitutions = if(isBound) typeSubstitutions else emptyMap()
		typeDeclaration?.withTypeParameters(specificTypeParameters, effectiveTypeSubstitutions) { specificTypeDeclaration ->
			specificType.typeDeclaration = specificTypeDeclaration
			specificTypeDeclaration.scope.addSubscriber(specificType)
		}
		return specificType
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

	override fun resolveTypeDeclarations() {
		super.resolveTypeDeclarations()
		if(typeDeclaration == null) {
			val sourceScope = enclosingType?.interfaceScope ?: scope
			typeDeclaration = sourceScope.getTypeDeclaration(name)
			if(typeDeclaration == null)
				context.addIssue(NotFound(source, "Type", name))
			if(typeDeclaration?.isBound != true)
				enclosingType?.setIsNonSpecificContext()
		}
		if(typeParameters.isNotEmpty()) {
			typeDeclaration?.withTypeParameters(typeParameters) { specificTypeDeclaration ->
				typeDeclaration = specificTypeDeclaration
			}
		}
		//TODO ask the definition to resolve all its member signatures
		typeDeclaration?.scope?.addSubscriber(this)
		val typeDeclaration = typeDeclaration
		if(typeDeclaration is TypeAlias)
			effectiveType = typeDeclaration.getEffectiveType()
	}

	override fun validate() {
		super.validate()
		if(isInSpecificContext)
			ensureSpecificDeclaration()
	}

	//TODO 'specific' naming is unclear
	private fun ensureSpecificDeclaration() {
		(typeDeclaration?.baseTypeDeclaration ?: typeDeclaration)?.let { typeDeclaration ->
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

	override fun getAbstractMemberDeclarations(): List<MemberDeclaration> {
		return typeDeclaration?.scope?.getAbstractMemberDeclarations() ?: listOf()
	}

	override fun getPropertiesToBeInitialized(): List<PropertyDeclaration> {
		return typeDeclaration?.scope?.getPropertiesToBeInitialized() ?: listOf()
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return typeDeclaration?.getConversionsFrom(sourceType) ?: listOf()
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
		if(enclosingType != null)
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
