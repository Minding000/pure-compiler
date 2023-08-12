package components.semantic_analysis.semantic_model.types

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDeclaration
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import errors.user.SignatureResolutionAmbiguityError
import java.util.*

class StaticType(val typeDeclaration: TypeDeclaration): Type(typeDeclaration.source, typeDeclaration.scope, true) {

	init {
		typeDeclaration.scope.addSubscriber(this)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): StaticType {
		// Assumption: StaticTypes don't have the recursion issues ObjectTypes have,
		//  since there can't be a StaticType inside a class definition
		lateinit var specificType: StaticType
		typeDeclaration.withTypeSubstitutions(typeSubstitutions) { specificTypeDeclaration ->
			specificType = StaticType(specificTypeDeclaration)
		}
		return specificType
	}

	override fun simplified(): Type = this

	fun withTypeParameters(typeParameters: List<Type>, onCompletion: (StaticType) -> Unit) {
		typeDeclaration.withTypeParameters(typeParameters) { specificTypeDeclaration ->
			onCompletion(StaticType(specificTypeDeclaration))
		}
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
		return typeDeclaration.getLinkedSuperType()?.isAssignableTo(targetType) ?: false
	}

	fun resolveInitializer(suppliedValues: List<Value> = emptyList()): Match? =
		resolveInitializer(emptyList(), emptyList(), emptyList(), suppliedValues)

	fun resolveInitializer(genericDefinitionTypes: List<TypeDeclaration>, suppliedDefinitionTypes: List<Type>, suppliedTypes: List<Type>,
						   suppliedValues: List<Value>): Match? {
		typeDeclaration.determineTypes()
		val matches = getMatchingInitializers(genericDefinitionTypes, suppliedDefinitionTypes, suppliedTypes, suppliedValues)
		if(matches.isEmpty())
			return null
		specificityPrecedenceLoop@for(match in matches) {
			for(otherMatch in matches) {
				if(otherMatch == match)
					continue
				if(!match.initializer.isMoreSpecificThan(otherMatch.initializer))
					continue@specificityPrecedenceLoop
			}
			for(parameterIndex in suppliedValues.indices) {
				val parameterType = match.initializer.getParameterTypeAt(parameterIndex)
				suppliedValues[parameterIndex].setInferredType(parameterType)
			}
			return match
		}
		throw SignatureResolutionAmbiguityError(matches.map { match -> match.initializer })
	}

	private fun getMatchingInitializers(genericDefinitionTypes: List<TypeDeclaration>, suppliedDefinitionTypes: List<Type>,
										suppliedTypes: List<Type>, suppliedValues: List<Value>): List<Match> {
		val matches = LinkedList<Match>()
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
				matches.add(Match(specificInitializer, definitionTypeSubstitutions))
		}
		return matches
	}

	class Match(val initializer: InitializerDefinition, val definitionTypeSubstitutions: Map<TypeDeclaration, Type>)

	fun getBaseTypeDeclaration(): TypeDeclaration {
		return typeDeclaration.baseTypeDeclaration ?: typeDeclaration
	}

	override fun equals(other: Any?): Boolean {
		if(other !is StaticType)
			return false
		if(typeDeclaration != other.typeDeclaration)
			return false
		return true
	}

	override fun hashCode(): Int {
		return typeDeclaration.hashCode()
	}

	override fun createLlvmType(constructor: LlvmConstructor): LlvmType {
		return constructor.pointerType
	}

	override fun toString(): String {
		return typeDeclaration.name
	}
}
