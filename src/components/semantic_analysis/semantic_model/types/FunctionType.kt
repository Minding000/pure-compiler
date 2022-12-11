package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import errors.user.SignatureResolutionAmbiguityError
import components.semantic_analysis.semantic_model.definitions.FunctionSignature
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.Element
import messages.Message
import java.util.*

//TODO include target instance type (optional) e.g. transform: String.() -> String
class FunctionType(override val source: Element): ObjectType(source, Linter.LiteralType.FUNCTION.className) {
	private val signatures = LinkedList<FunctionSignature>()
	var superFunctionType: FunctionType? = null

	constructor(source: Element, signature: FunctionSignature): this(source) {
		addSignature(signature)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		for(unit in units)
			unit.linkTypes(linter, scope)
		definition = linter.literalScopes[Linter.LiteralType.FUNCTION]?.resolveType(name)
		if(definition == null)
			linter.addMessage(source, "Type '$name' hasn't been declared yet.", Message.Type.ERROR)
	}

	fun hasSignature(signature: FunctionSignature): Boolean {
		return signatures.contains(signature) || superFunctionType?.hasSignature(signature) ?: false
	}

	fun addSignature(signature: FunctionSignature) {
		addUnits(signature)
		signatures.add(signature)
	}

	fun removeSignature(signature: FunctionSignature) {
		removeUnit(signature)
		signatures.remove(signature)
	}

	fun resolveSignature(suppliedValues: List<Value>): FunctionSignature? = resolveSignature(listOf(), suppliedValues)

	fun resolveSignature(suppliedTypes: List<Type> = listOf(), suppliedValues: List<Value> = listOf()): FunctionSignature? {
		val validSignatures = getMatchingSignatures(suppliedTypes, suppliedValues)
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
					if(validSignature.superFunctionSignature === validSuperSignature)
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
		if(targetType !is FunctionType)
			return Linter.LiteralType.ANY.matches(targetType)
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
