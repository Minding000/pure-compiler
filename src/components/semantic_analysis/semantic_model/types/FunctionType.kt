package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.FunctionSignature
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.Element
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.LiteralTypeNotFound
import java.util.*

class FunctionType(override val source: Element, scope: Scope): ObjectType(source, scope, Linter.SpecialType.FUNCTION.className) {
	private val signatures = LinkedList<FunctionSignature>()
	var superFunctionType: FunctionType? = null
		set(value) {
			field = value
			value?.let { superFunctionType ->
				for(signature in signatures) {
					for(superSignature in superFunctionType.signatures) {
						if(signature.fulfillsInheritanceRequirementsOf(superSignature)) {
							signature.superFunctionSignature = superSignature
							break
						}
					}
				}
			}
		}

	constructor(source: Element, scope: Scope, signature: FunctionSignature): this(source, scope) {
		addSignature(signature)
	}

	override fun linkTypes(linter: Linter) {
		interfaceScope.type = this
		for(unit in units)
			unit.linkTypes(linter)
		definition = Linter.SpecialType.FUNCTION.scope?.resolveType(name)
		if(definition == null)
			linter.addIssue(LiteralTypeNotFound(source, name))
	}

	fun hasSignatureOverriddenBy(subSignature: FunctionSignature): Boolean {
		for(signature in signatures) {
			if(subSignature.fulfillsInheritanceRequirementsOf(signature))
				return true
		}
		return superFunctionType?.hasSignatureOverriddenBy(subSignature) ?: false
	}

	fun addSignature(signature: FunctionSignature) {
		addUnits(signature)
		signatures.add(signature)
	}

	fun resolveSignature(linter: Linter, suppliedValues: List<Value>): FunctionSignature? =
		resolveSignature(linter, listOf(), suppliedValues)

	fun resolveSignature(linter: Linter, suppliedTypes: List<Type> = listOf(), suppliedValues: List<Value> = listOf()): FunctionSignature? {
		val validSignatures = getMatchingSignatures(linter, suppliedTypes, suppliedValues)
		if(validSignatures.isEmpty())
			return null
		specificityPrecedenceLoop@for(signature in validSignatures) {
			for(otherSignature in validSignatures) {
				if(otherSignature === signature)
					continue
				if(!signature.isMoreSpecificThan(otherSignature))
					continue@specificityPrecedenceLoop
			}
			for(parameterIndex in suppliedValues.indices)
				suppliedValues[parameterIndex].setInferredType(signature.parameterTypes[parameterIndex])
			return signature
		}
		throw SignatureResolutionAmbiguityError(validSignatures)
	}

	private fun getMatchingSignatures(linter: Linter, suppliedTypes: List<Type>, suppliedValues: List<Value>): List<FunctionSignature> {
		val validSignatures = LinkedList<FunctionSignature>()
		for(signature in signatures) {
			val typeSubstitutions = signature.getTypeSubstitutions(suppliedTypes, suppliedValues) ?: continue
			val specificSignature = if(typeSubstitutions.isEmpty())
				signature
			else
				signature.withTypeSubstitutions(linter, typeSubstitutions)
			if(specificSignature.accepts(suppliedValues))
				validSignatures.add(specificSignature)
		}
		superFunctionType?.let { superFunctionType ->
			val validSuperSignatures = superFunctionType.getMatchingSignatures(linter, suppliedTypes, suppliedValues)
			validSuperSignatures@for(validSuperSignature in validSuperSignatures) {
				for(validSignature in validSignatures)
					if(validSignature.superFunctionSignature === validSuperSignature)
						continue@validSuperSignatures
				validSignatures.add(validSuperSignature)
			}
		}
		return validSignatures
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): Type {
		val specificFunctionType = FunctionType(source, scope)
		for(signature in signatures)
			specificFunctionType.signatures.add(signature.withTypeSubstitutions(linter, typeSubstitutions))
		return specificFunctionType
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		return unresolvedSourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = resolveTypeAlias(unresolvedTargetType)
		if(targetType !is FunctionType)
			return Linter.SpecialType.ANY.matches(targetType)
		signatureAssignabilityCheck@for(requiredSignature in targetType.signatures) {
			for(availableSignature in signatures) {
				if(requiredSignature.accepts(availableSignature))
					continue@signatureAssignabilityCheck
			}
			return false
		}
		return true
	}

	override fun equals(other: Any?): Boolean {
		if(other !is FunctionType)
			return false
		if(other.signatures.size != signatures.size)
			return false
		for(signature in signatures) {
			if(!other.signatures.contains(signature))
				return false
		}
		return true
	}

	override fun hashCode(): Int {
		return signatures.hashCode()
	}

	override fun toString(): String {
		return signatures.joinToString(" & ")
	}
}
