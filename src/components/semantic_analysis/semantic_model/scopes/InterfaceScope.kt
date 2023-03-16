package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.MemberDeclaration
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Instance
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import errors.user.SignatureResolutionAmbiguityError
import java.util.*

class InterfaceScope(val isStatic: Boolean = false): Scope() {
	lateinit var type: Type
	private val types = HashMap<String, TypeDefinition>()
	private val values = HashMap<String, InterfaceMember>()
	private val initializers = LinkedList<InitializerDefinition>()

	fun hasType(type: TypeDefinition): Boolean = types.containsValue(type)

	fun hasValue(valueName: String): Boolean = values.containsKey(valueName)
	fun hasValue(value: InterfaceMember): Boolean = values.containsValue(value)

	fun hasInstance(name: String): Boolean {
		for((_, value) in values) {
			if(value is Instance && value.name == name)
				return true
		}
		return false
	}

	override fun subscribe(type: Type) {
		super.subscribe(type)
		for((_, typeDefinition) in types)
			type.onNewType(typeDefinition)
		for((_, value) in values)
			type.onNewValue(value)
		for(initializer in initializers)
			type.onNewInitializer(initializer)
	}

	fun addType(type: TypeDefinition) {
		if(!types.containsKey(type.name)) {
			types[type.name] = type
			onNewType(type)
		}
	}

	fun addValue(value: InterfaceMember) {
		if(!values.containsKey(value.name)) {
			values[value.name] = value
			onNewValue(value)
		}
	}

	fun addInitializer(initializer: InitializerDefinition) {
		if(!initializers.contains(initializer)) {
			initializers.add(initializer)
			onNewInitializer(initializer)
		}
	}

	override fun resolveValue(name: String): InterfaceMember? {
		return values[name]
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name]
	}

	fun getSuperInitializer(initializer: InitializerDefinition): InitializerDefinition? {
		for(superInitializer in initializers) {
			if(initializer.fulfillsInheritanceRequirementsOf(superInitializer))
				return superInitializer
		}
		return null
	}

	fun resolveInitializer(suppliedValues: List<Value>): MatchResult? =
		resolveInitializer(listOf(), listOf(), listOf(), suppliedValues)

	fun resolveInitializer(genericDefinitionTypes: List<TypeDefinition>, suppliedDefinitionTypes: List<Type>,
						   suppliedTypes: List<Type>, suppliedValues: List<Value>): MatchResult? {
		val matches = getMatchingInitializers(genericDefinitionTypes, suppliedDefinitionTypes, suppliedTypes, suppliedValues)
		if(matches.isEmpty())
			return null
		specificityPrecedenceLoop@for(match in matches) {
			for(otherMatch in matches) {
				if(otherMatch == match)
					continue
				if(!match.signature.isMoreSpecificThan(otherMatch.signature))
					continue@specificityPrecedenceLoop
			}
			for(parameterIndex in suppliedValues.indices)
				suppliedValues[parameterIndex].setInferredType(match.signature.parameters[parameterIndex].type)
			return match
		}
		throw SignatureResolutionAmbiguityError(matches.map { match -> match.signature })
	}

	private fun getMatchingInitializers(genericDefinitionTypes: List<TypeDefinition>, suppliedDefinitionTypes: List<Type>,
										suppliedTypes: List<Type>, suppliedValues: List<Value>): List<MatchResult> {
		val validSignatures = LinkedList<MatchResult>()
		for(initializer in initializers) {
			var specificInitializer = initializer
			val definitionTypeSubstitutions = initializer.getDefinitionTypeSubstitutions(genericDefinitionTypes, suppliedDefinitionTypes,
				suppliedValues) ?: continue
			if(definitionTypeSubstitutions.isNotEmpty())
				specificInitializer = specificInitializer.withTypeSubstitutions(definitionTypeSubstitutions) //TODO the copied unit should be added to units (same for functions and operators)
			val typeSubstitutions = specificInitializer.getTypeSubstitutions(suppliedTypes, suppliedValues) ?: continue
			if(typeSubstitutions.isNotEmpty())
				specificInitializer = specificInitializer.withTypeSubstitutions(typeSubstitutions) //TODO the copied unit should be added to units (same for functions and operators)
			if(specificInitializer.accepts(suppliedValues))
				validSignatures.add(MatchResult(specificInitializer, definitionTypeSubstitutions))
		}
		return validSignatures
	}

	class MatchResult(val signature: InitializerDefinition, val definitionTypeSubstitutions: Map<TypeDefinition, Type>)

	fun getAbstractMembers(): List<MemberDeclaration> = type.getAbstractMembers()
	fun getPropertiesToBeInitialized(): List<PropertyDeclaration> = type.getPropertiesToBeInitialized()

	fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return initializers.filter { initializer -> initializer.isConvertingFrom(sourceType) }
	}

	override fun toString(): String {
		return "InterfaceScope of $type"
	}
}
