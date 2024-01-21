package components.semantic_model.types

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.semantic_model.context.ComparisonResult
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.values.Value
import errors.user.SignatureResolutionAmbiguityError
import java.util.*

class StaticType(val typeDeclaration: TypeDeclaration): Type(typeDeclaration.source, typeDeclaration.scope, true) {

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>) = this
	override fun createCopyWithTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>) = this

	override fun simplified(): Type = this

	override fun getInitializers(): List<InitializerDefinition> {
		return typeDeclaration.scope.initializers
	}

	override fun getAllInitializers(): List<InitializerDefinition> {
		return typeDeclaration.scope.initializers
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		val typeDeclaration = typeDeclaration.scope.getTypeDeclaration(name)
		if(typeDeclaration?.isBound == true)
			return null
		return typeDeclaration
	}

	override fun getValueDeclaration(name: String): ValueDeclaration.Match? {
		//TODO only return static value declarations here (write tests!)
		return typeDeclaration.scope.getValueDeclaration(name)
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

	fun getInitializer(suppliedValues: List<Value> = emptyList()): Match? =
		getInitializer(emptyList(), emptyList(), emptyList(), suppliedValues)

	fun getInitializer(globalTypeParameters: List<TypeDeclaration>, suppliedGlobalTypes: List<Type>, suppliedLocalTypes: List<Type>,
					   suppliedValues: List<Value>): Match? {
		typeDeclaration.determineTypes()
		val matches = getMatchingInitializers(globalTypeParameters, suppliedGlobalTypes, suppliedLocalTypes, suppliedValues)
		if(matches.isEmpty())
			return null
		specificityPrecedenceLoop@for(match in matches) {
			for(otherMatch in matches) {
				if(otherMatch == match)
					continue
				if(match.compareSpecificity(otherMatch) != ComparisonResult.HIGHER)
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

	private fun getMatchingInitializers(globalTypeParameters: List<TypeDeclaration>, suppliedGlobalTypes: List<Type>,
										suppliedLocalTypes: List<Type>, suppliedValues: List<Value>): List<Match> {
		val matches = LinkedList<Match>()
		for(initializer in interfaceScope.getDirectInitializers()) {
			//TODO specialized issue if global type parameter can't be determined for any initializer, but al least one exists
			val globalTypeSubstitutions = initializer.getGlobalTypeSubstitutions(globalTypeParameters, suppliedGlobalTypes,
				suppliedValues) ?: continue
			//TODO specialized issue if local type parameter can't be determined for any initializer, but al least one exists (same for functions)
			val localTypeSubstitutions = initializer.getLocalTypeSubstitutions(globalTypeSubstitutions, suppliedLocalTypes,
				suppliedValues) ?: continue
			val conversions = HashMap<Value, InitializerDefinition>()
			if(initializer.accepts(globalTypeSubstitutions, localTypeSubstitutions, suppliedValues, conversions))
				matches.add(Match(initializer, globalTypeSubstitutions, conversions))
		}
		return matches
	}

	class Match(val initializer: InitializerDefinition, val globalTypeSubstitutions: Map<TypeDeclaration, Type>,
		val conversions: Map<Value, InitializerDefinition>) {

		fun compareSpecificity(otherMatch: Match): ComparisonResult {
			val initializerComparisonResult = initializer.compareSpecificity(otherMatch.initializer)
			if(initializerComparisonResult != ComparisonResult.SAME)
				return initializerComparisonResult
			if(conversions.size < otherMatch.conversions.size)
				return ComparisonResult.HIGHER
			if(conversions.size > otherMatch.conversions.size)
				return ComparisonResult.LOWER
			if(!initializer.isConverting && otherMatch.initializer.isConverting)
				return ComparisonResult.HIGHER
			if(initializer.isConverting && !otherMatch.initializer.isConverting)
				return ComparisonResult.LOWER
			return ComparisonResult.SAME
		}
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
