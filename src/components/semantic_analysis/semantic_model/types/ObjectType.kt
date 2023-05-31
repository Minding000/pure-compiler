package components.semantic_analysis.semantic_model.types

import components.compiler.targets.llvm.LlvmContext
import components.compiler.targets.llvm.LlvmTypeReference
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.definitions.*
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.definition.TypeParameterCountMismatch
import logger.issues.definition.TypeParameterNotAssignable
import logger.issues.resolution.NotFound

open class ObjectType(override val source: SyntaxTreeNode, scope: Scope, val enclosingType: ObjectType?, val typeParameters: List<Type>,
					  val name: String, var definition: TypeDefinition? = null): Type(source, scope) {
	private var isInSpecificContext = true

	constructor(source: SyntaxTreeNode, surroundingScope: Scope, name: String): this(source, surroundingScope, null, listOf(), name)

	constructor(definition: TypeDefinition):
		this(definition.source, definition.scope, null, listOf(), definition.name, definition)

	constructor(typeParameters: List<Type>, definition: TypeDefinition):
		this(definition.source, definition.scope, null, typeParameters, definition.name, definition)

	init {
		addSemanticModels(enclosingType)
		addSemanticModels(typeParameters)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Type {
		val substituteType = typeSubstitutions[definition]
		if(substituteType != null)
			return substituteType
		val isBound = definition?.isBound == true
		if(!isBound && typeParameters.isEmpty())
			return this
		val specificTypeParameters = typeParameters.map { typeParameter ->
			typeParameter.withTypeSubstitutions(typeSubstitutions) }
		//TODO this might be nicer if it was written with a return in the callback
		// -> withTypeParameter needs to have inline modifier
		val specificType = ObjectType(source, scope, enclosingType, specificTypeParameters, name)
		val typeSubstitutions = if(isBound) typeSubstitutions else mapOf()
		definition?.withTypeParameters(specificTypeParameters, typeSubstitutions) { specificDefinition ->
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

	override fun resolveDefinitions() {
		super.resolveDefinitions()
		if(definition == null) {
			val sourceScope = enclosingType?.interfaceScope ?: scope
			definition = sourceScope.resolveType(name)
			if(definition == null)
				context.addIssue(NotFound(source, "Type", name))
			if(definition?.isBound != true)
				enclosingType?.setIsNonSpecificContext()
		}
		if(typeParameters.isNotEmpty()) {
			definition?.withTypeParameters(typeParameters) { specificDefinition ->
				definition = specificDefinition
			}
		}
		//TODO ask the definition to resolve all its member signatures
		definition?.scope?.subscribe(this)
		val definition = definition
		if(definition is TypeAlias)
			effectiveType = definition.getEffectiveType()
	}

	override fun validate() {
		super.validate()
		if(isInSpecificContext)
			ensureSpecificDefinition()
	}

	private fun ensureSpecificDefinition() {
		(definition?.baseDefinition ?: definition)?.let { definition ->
			val genericTypes = definition.scope.getGenericTypeDefinitions()
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

	override fun isInstanceOf(type: SpecialType): Boolean {
		if(type.matches(this))
			return true
		return definition?.getLinkedSuperType()?.isInstanceOf(type) ?: false
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		if(unresolvedSourceType is StaticType)
			return false
		return unresolvedSourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
		if(definition is TypeAlias)
			return effectiveType.isAssignableTo(targetType)
		if(targetType is StaticType || targetType is FunctionType)
			return false
		if(targetType !is ObjectType)
			return targetType.accepts(this)
		if(equals(targetType))
			return true
		return definition?.getLinkedSuperType()?.isAssignableTo(targetType) ?: false
	}

	override fun getAbstractMembers(): List<MemberDeclaration> {
		return definition?.scope?.getAbstractMembers() ?: listOf()
	}

	override fun getPropertiesToBeInitialized(): List<PropertyDeclaration> {
		return definition?.scope?.getPropertiesToBeInitialized() ?: listOf()
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return definition?.getConversionsFrom(sourceType) ?: listOf()
	}

	override fun equals(other: Any?): Boolean {
		if(other !is Type)
			return false
		val otherType = other.effectiveType
		if(definition is TypeAlias)
			return effectiveType == otherType
		if(otherType !is ObjectType)
			return false
		if(definition != otherType.definition)
			return false
		if(definition == null && name != otherType.name)
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

	override fun getLlvmReference(llvmContext: LlvmContext): LlvmTypeReference {
		if(SpecialType.INTEGER.matches(this))
			return llvmContext.i32Type
		if(SpecialType.NOTHING.matches(this))
			return llvmContext.voidType
		if(SpecialType.NEVER.matches(this))
			return llvmContext.voidType
		return super.getLlvmReference(llvmContext)
	}
}
