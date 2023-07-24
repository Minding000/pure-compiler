package components.semantic_analysis.semantic_model.types

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
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

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): StaticType {
		// Assumption: StaticTypes don't have the recursion issues ObjectTypes have,
		//  since there can't be a StaticType inside a class definition
		lateinit var specificType: StaticType
		definition.withTypeSubstitutions(typeSubstitutions) { specificDefinition ->
			specificType = StaticType(specificDefinition)
		}
		return specificType
	}

	override fun simplified(): Type = this

	fun withTypeParameters(typeParameters: List<Type>, onCompletion: (StaticType) -> Unit) {
		definition.withTypeParameters(typeParameters) { specificDefinition ->
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
		val targetType = unresolvedTargetType.effectiveType
		if(targetType is FunctionType)
			return false
		if(targetType !is StaticType)
			return targetType.accepts(this)
		if(equals(targetType))
			return true
		return definition.getLinkedSuperType()?.isAssignableTo(targetType) ?: false
	}

	fun resolveInitializer(suppliedValues: List<Value>): MatchResult? =
		resolveInitializer(listOf(), listOf(), listOf(), suppliedValues)

	fun resolveInitializer(genericDefinitionTypes: List<TypeDefinition>, suppliedDefinitionTypes: List<Type>,
						   suppliedTypes: List<Type>, suppliedValues: List<Value>): MatchResult? {
		definition.determineTypes()
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
		for(initializer in interfaceScope.initializers) {
			var specificInitializer = initializer
			val definitionTypeSubstitutions = initializer.getDefinitionTypeSubstitutions(genericDefinitionTypes, suppliedDefinitionTypes,
				suppliedValues) ?: continue
			if(definitionTypeSubstitutions.isNotEmpty())
				specificInitializer = specificInitializer.withTypeSubstitutions(definitionTypeSubstitutions) //TODO the copied semanticModel should be added to semanticModels (same for functions and operators)
			val typeSubstitutions = specificInitializer.getTypeSubstitutions(suppliedTypes, suppliedValues) ?: continue
			if(typeSubstitutions.isNotEmpty())
				specificInitializer = specificInitializer.withTypeSubstitutions(typeSubstitutions) //TODO the copied semanticModel should be added to semanticModels (same for functions and operators)
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

	override fun createLlvmType(constructor: LlvmConstructor): LlvmType {
		return constructor.createPointerType(definition.llvmType)
	}

	override fun toString(): String {
		return definition.name
	}
}
