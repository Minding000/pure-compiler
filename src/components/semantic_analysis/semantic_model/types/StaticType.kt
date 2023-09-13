package components.semantic_analysis.semantic_model.types

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.semantic_analysis.semantic_model.declarations.InitializerDefinition
import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import errors.user.SignatureResolutionAmbiguityError
import java.util.*

class StaticType(val typeDeclaration: TypeDeclaration): Type(typeDeclaration.source, typeDeclaration.scope, true) {

	init {
		typeDeclaration.scope.addSubscriber(this)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>) = this
	override fun createCopyWithTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>) = this

	override fun simplified(): Type = this

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

	private fun getMatchingInitializers(globalTypeParameters: List<TypeDeclaration>, suppliedGlobalTypes: List<Type>,
										suppliedLocalTypes: List<Type>, suppliedValues: List<Value>): List<Match> {
		val matches = LinkedList<Match>()
		for(initializer in interfaceScope.initializers) {
			val globalTypeSubstitutions = initializer.getGlobalTypeSubstitutions(globalTypeParameters, suppliedGlobalTypes,
				suppliedValues) ?: continue
			val localTypeSubstitutions = initializer.getLocalTypeSubstitutions(globalTypeSubstitutions, suppliedLocalTypes,
				suppliedValues) ?: continue
			if(initializer.accepts(globalTypeSubstitutions, localTypeSubstitutions, suppliedValues))
				matches.add(Match(initializer, globalTypeSubstitutions))
		}
		return matches
	}

	class Match(val initializer: InitializerDefinition, val globalTypeSubstitutions: Map<TypeDeclaration, Type>)

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
