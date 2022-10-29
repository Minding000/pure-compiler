package linting.semantic_model.types

import errors.user.SignatureResolutionAmbiguityError
import linting.semantic_model.definitions.FunctionSignature
import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.values.Value
import parsing.syntax_tree.general.Element
import java.util.*

class FunctionType(override val source: Element): Type(source) {
	private val signatures = LinkedList<FunctionSignature>()
	var superFunctionType: FunctionType? = null

	constructor(source: Element, signature: FunctionSignature): this(source) {
		addSignature(signature)
	}

	fun hasSignature(signature: FunctionSignature): Boolean {
		return signatures.contains(signature) || superFunctionType?.hasSignature(signature) ?: false
	}

	fun addSignature(signature: FunctionSignature) {
		units.add(signature)
		signatures.add(signature)
	}

	fun removeSignature(signature: FunctionSignature) {
		units.remove(signature)
		signatures.remove(signature)
	}

	fun resolveSignature(suppliedValues: List<Value>): FunctionSignature? = resolveSignature(listOf(), suppliedValues)

	fun resolveSignature(suppliedTypes: List<Type> = listOf(), suppliedValues: List<Value> = listOf()): FunctionSignature? {
		val validSignatures = getMatchingSignatures(suppliedTypes, suppliedValues)
		if(validSignatures.isEmpty())
			return null
		specificityPrecedenceLoop@ for(signature in validSignatures) {
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

	private fun getMatchingSignatures(suppliedTypes: List<Type>, suppliedValues: List<Value>): List<FunctionSignature> {
		val validSignatures = LinkedList<FunctionSignature>()
		for(signature in signatures) {
			val typeSubstitutions = signature.getTypeSubstitutions(suppliedTypes, suppliedValues) ?: continue
			val specificSignature = if(typeSubstitutions.isEmpty())
				signature
			else
				signature.withTypeSubstitutions(typeSubstitutions)
			if(specificSignature.accepts(suppliedValues))
				validSignatures.add(specificSignature)
		}
		superFunctionType?.let { superFunctionType ->
			val validSuperSignatures = superFunctionType.getMatchingSignatures(suppliedTypes, suppliedValues)
			validSuperSignatures@for(validSuperSignature in validSuperSignatures) {
				for(validSignature in validSignatures)
					if(validSignature.superFunctionSignature == validSuperSignature)
						continue@validSuperSignatures
				validSignatures.add(validSuperSignature)
			}
		}
		return validSignatures
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Type {
		val specificFunctionType = FunctionType(source)
		for(signature in signatures)
			specificFunctionType.signatures.add(signature.withTypeSubstitutions(typeSubstitutions))
		return specificFunctionType
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		return unresolvedSourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = resolveTypeAlias(unresolvedTargetType)
		if(targetType is ObjectType)
			return false
		if(targetType !is FunctionType)
			return targetType.accepts(this)
		return equals(targetType)
	}

	override fun equals(other: Any?): Boolean {
		if(other !is FunctionType)
			return false
		return true
	}

	override fun hashCode(): Int {
		return signatures.hashCode()
	}

	override fun toString(): String {
		return signatures.joinToString("\n")
	}
}
