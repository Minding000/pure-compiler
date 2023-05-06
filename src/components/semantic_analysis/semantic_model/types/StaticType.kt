package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import errors.user.SignatureResolutionAmbiguityError
import java.util.*

class StaticType(val definition: TypeDefinition): Type(definition.source, definition.scope, true) {

	init {
		definition.scope.subscribe(this)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): StaticType {
		// Assumption: StaticTypes don't have the recursion issues ObjectTypes have,
		//  since there can't be a StaticType inside a class definition
		lateinit var specificType: StaticType
		definition.withTypeSubstitutions(linter, typeSubstitutions) { specificDefinition ->
			specificType = StaticType(specificDefinition)
		}
		return specificType
	}

	override fun simplified(): Type = this

	fun withTypeParameters(linter: Linter, typeParameters: List<Type>, onCompletion: (StaticType) -> Unit) {
		definition.withTypeParameters(linter, typeParameters) { specificDefinition ->
			onCompletion(StaticType(specificDefinition))
		}
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

	fun resolveInitializer(linter: Linter, suppliedValues: List<Value>): MatchResult? =
		resolveInitializer(linter, listOf(), listOf(), listOf(), suppliedValues)

	fun resolveInitializer(linter: Linter, genericDefinitionTypes: List<TypeDefinition>, suppliedDefinitionTypes: List<Type>,
						   suppliedTypes: List<Type>, suppliedValues: List<Value>): MatchResult? {
		definition.determineTypes(linter)
		val matches = getMatchingInitializers(linter, genericDefinitionTypes, suppliedDefinitionTypes, suppliedTypes, suppliedValues)
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

	private fun getMatchingInitializers(linter: Linter, genericDefinitionTypes: List<TypeDefinition>, suppliedDefinitionTypes: List<Type>,
										suppliedTypes: List<Type>, suppliedValues: List<Value>): List<MatchResult> {
		val validSignatures = LinkedList<MatchResult>()
		for(initializer in interfaceScope.initializers) {
			var specificInitializer = initializer
			val definitionTypeSubstitutions = initializer.getDefinitionTypeSubstitutions(genericDefinitionTypes, suppliedDefinitionTypes,
				suppliedValues) ?: continue
			if(definitionTypeSubstitutions.isNotEmpty())
				specificInitializer = specificInitializer.withTypeSubstitutions(linter, definitionTypeSubstitutions) //TODO the copied unit should be added to units (same for functions and operators)
			val typeSubstitutions = specificInitializer.getTypeSubstitutions(suppliedTypes, suppliedValues) ?: continue
			if(typeSubstitutions.isNotEmpty())
				specificInitializer = specificInitializer.withTypeSubstitutions(linter, typeSubstitutions) //TODO the copied unit should be added to units (same for functions and operators)
			if(specificInitializer.accepts(suppliedValues))
				validSignatures.add(MatchResult(specificInitializer, definitionTypeSubstitutions))
		}
		return validSignatures
	}

	class MatchResult(val signature: InitializerDefinition, val definitionTypeSubstitutions: Map<TypeDefinition, Type>)

	fun getBaseDefinition(): TypeDefinition {
		return definition.baseDefinition ?: definition
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
