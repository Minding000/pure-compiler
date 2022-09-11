package linter.elements.literals

import linter.elements.definitions.FunctionSignature
import linter.elements.general.Unit
import parsing.ast.general.Element
import java.util.*

class FunctionType(val source: Element): Type() {
	private val signatures = LinkedList<FunctionSignature>()

	constructor(source: Element, signature: FunctionSignature): this(source) {
		addSignature(signature)
	}

	fun addSignature(signature: FunctionSignature) {
		units.add(signature)
		signatures.add(signature)
	}

	fun resolveSignature(suppliedTypes: List<Type?>): FunctionSignature? {
		val validSignatures = LinkedList<FunctionSignature>()
		for(signature in signatures) {
			if(signature.accepts(suppliedTypes))
				validSignatures.add(signature)
		}
		if(validSignatures.isEmpty())
			return null
		specificityPrecedenceLoop@for(signature in validSignatures) {
			for(otherSignature in validSignatures) {
				if(otherSignature === signature)
					continue
				if(otherSignature.parameterTypes == signature.parameterTypes)
					continue@specificityPrecedenceLoop
				if(!otherSignature.accepts(signature.parameterTypes))
					continue@specificityPrecedenceLoop
			}
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

	class SignatureResolutionAmbiguityError(val signatures: List<Unit>): Error()
}