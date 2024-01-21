package components.semantic_model.types

import components.semantic_model.context.ComparisonResult
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Function
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.modifiers.OverridingFunctionReturnTypeNotAssignable
import logger.issues.resolution.LiteralTypeNotFound
import java.util.*

class FunctionType(override val source: SyntaxTreeNode, scope: Scope): ObjectType(source, scope, SpecialType.FUNCTION.className) {
	var associatedFunction: Function? = null
	val signatures = LinkedList<FunctionSignature>()
	var superFunctionType: FunctionType? = null
	private var hasDeterminedSuperFunction = false
	private var hasDeterminedSuperSignatures = false

	constructor(source: SyntaxTreeNode, scope: Scope, signature: FunctionSignature): this(source, scope) {
		addSignature(signature)
	}

	override fun resolveTypeDeclarations() {
		interfaceScope.type = this
		for(semanticModel in semanticModels)
			semanticModel.determineTypes()
		val typeDeclaration = SpecialType.FUNCTION.fileScope?.getTypeDeclaration(name)
		typeDeclarationCache = typeDeclaration
		if(typeDeclaration == null)
			context.addIssue(LiteralTypeNotFound(source, name))
	}

	fun addSignature(signature: FunctionSignature) {
		addSemanticModels(signature)
		signatures.add(signature)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): FunctionType {
		if(typeSubstitutions.isEmpty())
			return this
		return createCopyWithTypeSubstitutions(typeSubstitutions)
	}

	override fun createCopyWithTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): FunctionType {
		val specificFunctionType = FunctionType(source, scope)
		determineSuperType()
		specificFunctionType.associatedFunction = associatedFunction
		specificFunctionType.superFunctionType = superFunctionType?.withTypeSubstitutions(typeSubstitutions)
		specificFunctionType.hasDeterminedSuperFunction = hasDeterminedSuperFunction
		specificFunctionType.hasDeterminedSuperSignatures = hasDeterminedSuperSignatures
		for(signature in signatures)
			specificFunctionType.signatures.add(signature.withTypeSubstitutions(typeSubstitutions))
		return specificFunctionType
	}

	fun getSignature(suppliedValues: List<Value>) = getSignature(emptyList(), suppliedValues)

	fun getSignature(suppliedLocalTypes: List<Type> = emptyList(), suppliedValues: List<Value> = emptyList()): Match? {
		val matches = getMatchingSignatures(suppliedLocalTypes, suppliedValues)
		if(matches.isEmpty())
			return null
		specificityPrecedenceLoop@for(match in matches) {
			for(otherMatch in matches) {
				if(otherMatch === match)
					continue
				if(match.compareSpecificity(otherMatch) != ComparisonResult.HIGHER)
					continue@specificityPrecedenceLoop
			}
			for(parameterIndex in suppliedValues.indices) {
				val parameterType = match.signature.getParameterTypeAt(parameterIndex)
				suppliedValues[parameterIndex].setInferredType(parameterType)
			}
			return match
		}
		throw SignatureResolutionAmbiguityError(matches.map { match -> match.signature })
	}

	private fun getMatchingSignatures(suppliedLocalTypes: List<Type>, suppliedValues: List<Value>): List<Match> {
		val matches = LinkedList<Match>()
		for(signature in signatures) {
			val localTypeSubstitutions = signature.getLocalTypeSubstitutions(suppliedLocalTypes, suppliedValues) ?: continue
			val conversions = HashMap<Value, InitializerDefinition>()
			if(signature.accepts(localTypeSubstitutions, suppliedValues, conversions))
				matches.add(Match(signature, localTypeSubstitutions, conversions))
		}
		determineSuperType()
		determineSuperSignatures()
		val superFunctionType = superFunctionType
		if(superFunctionType != null) {
			val superMatches = superFunctionType.getMatchingSignatures(suppliedLocalTypes, suppliedValues)
			validSuperSignatures@for(superMatch in superMatches) {
				for(match in matches)
					if(match.signature.overrides(superMatch.signature))
						continue@validSuperSignatures
				matches.add(superMatch)
			}
		}
		return matches
	}

	fun determineSuperType() {
		if(hasDeterminedSuperFunction)
			return
		try {
			val function = associatedFunction ?: return
			val superMemberType = function.associatedTypeDeclaration?.superType?.getValueDeclaration(function.name)?.type ?: return
			superFunctionType = superMemberType as? FunctionType
		} finally {
			hasDeterminedSuperFunction = true
		}
	}

	fun determineSuperSignatures() {
		if(hasDeterminedSuperSignatures)
			return
		try {
			val function = associatedFunction ?: return
			val superFunctionType = superFunctionType ?: return
			for(signature in signatures) {
				superSignatureLoop@for(superSignature in superFunctionType.signatures) {
					if(signature.parameterTypes.size != superSignature.parameterTypes.size)
						continue
					for(parameterIndex in signature.parameterTypes.indices) {
						val superParameterType = superSignature.parameterTypes[parameterIndex] ?: continue
						val baseParameterType = signature.parameterTypes[parameterIndex] ?: continue
						if(!baseParameterType.accepts(superParameterType))
							continue@superSignatureLoop
					}
					signature.superFunctionSignature = superSignature
					if(!signature.returnType.isAssignableTo(superSignature.returnType)) {
						val implementation = signature.associatedImplementation
							?: throw CompilerError("Encountered member signature without implementation.")
						val superImplementation = superSignature.associatedImplementation
							?: throw CompilerError("Encountered member signature without implementation.")
						signature.context.addIssue(
							OverridingFunctionReturnTypeNotAssignable(implementation.source, function.memberType,
								implementation.toString(), superImplementation.toString())
						)
					}
					break
				}
			}
		} finally {
			hasDeterminedSuperSignatures = true
		}
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

	class Match(val signature: FunctionSignature, val localTypeSubstitutions: Map<TypeDeclaration, Type>,
				val conversions: Map<Value, InitializerDefinition>) {
		val returnType = signature.returnType.withTypeSubstitutions(localTypeSubstitutions)

		fun compareSpecificity(otherMatch: Match): ComparisonResult {
			val signatureComparisonResult = signature.compareSpecificity(otherMatch.signature)
			if(signatureComparisonResult != ComparisonResult.SAME)
				return signatureComparisonResult
			if(conversions.size < otherMatch.conversions.size)
				return ComparisonResult.HIGHER
			if(conversions.size > otherMatch.conversions.size)
				return ComparisonResult.LOWER
			return ComparisonResult.SAME
		}
	}
}
