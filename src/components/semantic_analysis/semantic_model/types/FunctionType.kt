package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.definitions.FunctionSignature
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.LiteralTypeNotFound
import java.util.*

class FunctionType(override val source: SyntaxTreeNode, scope: Scope): ObjectType(source, scope, SpecialType.FUNCTION.className) {
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

	constructor(source: SyntaxTreeNode, scope: Scope, signature: FunctionSignature): this(source, scope) {
		addSignature(signature)
	}

	override fun resolveDefinitions() {
		interfaceScope.type = this
		for(semanticModel in semanticModels)
			semanticModel.determineTypes()
		definition = SpecialType.FUNCTION.scope?.resolveType(name)
		definition?.scope?.subscribe(this)
		if(definition == null)
			context.addIssue(LiteralTypeNotFound(source, name))
	}

	fun addSignature(signature: FunctionSignature) {
		addSemanticModels(signature)
		signatures.add(signature)
	}

	fun resolveSignature(suppliedValues: List<Value>): FunctionSignature? =
		resolveSignature(listOf(), suppliedValues)

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
			for(parameterIndex in suppliedValues.indices) {
				val parameterType = if(parameterIndex < signature.fixedParameterTypes.size)
					signature.fixedParameterTypes[parameterIndex]
				else
					signature.variadicParameterType
				suppliedValues[parameterIndex].setInferredType(parameterType)
			}
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
		val specificFunctionType = FunctionType(source, scope)
		for(signature in signatures)
			specificFunctionType.signatures.add(signature.withTypeSubstitutions(typeSubstitutions))
		return specificFunctionType
	}

	override fun accepts(unresolvedSourceType: Type): Boolean {
		return unresolvedSourceType.isAssignableTo(this)
	}

	override fun isAssignableTo(unresolvedTargetType: Type): Boolean {
		val targetType = unresolvedTargetType.effectiveType
		if(targetType !is FunctionType)
			return SpecialType.ANY.matches(targetType)
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
