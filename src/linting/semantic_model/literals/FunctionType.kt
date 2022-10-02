package linting.semantic_model.literals

import linting.semantic_model.definitions.FunctionSignature
import linting.semantic_model.general.Unit
import linting.semantic_model.values.Value
import parsing.syntax_tree.general.Element
import java.util.*

class FunctionType(val source: Element) : Type() {
	private val signatures = LinkedList<FunctionSignature>()
	var superFunctionType: FunctionType? = null

	constructor(source: Element, signature: FunctionSignature) : this(source) {
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

	private fun getMatchingSignatures(suppliedValues: List<Value>): List<FunctionSignature> {
		val validSignatures = LinkedList<FunctionSignature>()
		for(signature in signatures) {
			if(signature.accepts(suppliedValues))
				validSignatures.add(signature)
		}
		superFunctionType?.let { superFunctionType ->
			val validSuperSignatures = superFunctionType.getMatchingSignatures(suppliedValues)
			validSuperSignatures@for(validSuperSignature in validSuperSignatures) {
				for(validSignature in validSignatures)
					if(validSignature.superFunctionSignature == validSuperSignature)
						continue@validSuperSignatures
				validSignatures.add(validSuperSignature)
			}
		}
		return validSignatures
	}

	fun resolveSignature(suppliedValues: List<Value>): FunctionSignature? {
		val validSignatures = getMatchingSignatures(suppliedValues)
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

	override fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): Type {
		val specificFunctionType = FunctionType(source)
		for(signature in signatures)
			specificFunctionType.signatures.add(signature.withTypeSubstitutions(typeSubstitution))
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

	class SignatureResolutionAmbiguityError(val signatures: List<Unit>) : Error()
}