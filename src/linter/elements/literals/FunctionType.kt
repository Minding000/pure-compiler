package linter.elements.literals

import linter.elements.definitions.FunctionSignature
import linter.elements.general.Unit
import parsing.ast.general.Element
import java.util.*

class FunctionType(val source: Element): Type() {
	private val signatures = LinkedList<FunctionSignature>()

	constructor(source: Element, signature: FunctionSignature): this(source) {
		signatures.add(signature)
	}

	fun resolveSignature(suppliedTypes: List<Type?>): FunctionSignature? {
		val validSignatures = LinkedList<FunctionSignature>()
		for(signature in signatures) {
			if(signature.accepts(scope, suppliedTypes))
				validSignatures.add(signature)
		}
		if(validSignatures.isEmpty())
			return null
		signatureCheck@for(signature in validSignatures) {
			for(otherSignature in validSignatures) {
				if(otherSignature == signature)
					continue
				if(!otherSignature.accepts(scope, signature.parameterTypes))
					continue@signatureCheck
			}
			return signature
		}
		throw SignatureResolutionAmbiguityError(validSignatures)
	}

	override fun withTypeSubstitutions(typeSubstitution: Map<Type, Type>): Type {
		val specificFunctionType = FunctionType(source)
		for(signature in signatures)
			signature.withTypeSubstitutions(typeSubstitution)
		return specificFunctionType
	}

	override fun accepts(sourceType: Type): Boolean {
		return sourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(targetType: Type): Boolean {
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

	class SignatureResolutionAmbiguityError(val signatures: List<Unit>): Error()
}