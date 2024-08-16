package components.semantic_model.declarations

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmType
import components.semantic_model.context.ComparisonResult
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.BlockScope
import components.semantic_model.types.*
import components.semantic_model.values.NumberLiteral
import components.semantic_model.values.Operator
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import logger.issues.resolution.ConversionAmbiguity
import util.combineOrUnion
import java.util.*
import kotlin.math.max

class FunctionSignature(override val source: SyntaxTreeNode, override val scope: BlockScope,
						val localTypeParameters: List<GenericTypeDeclaration>, val parameterTypes: List<Type?>, returnType: Type?,
						val whereClauseConditions: List<WhereClauseCondition> = emptyList(),
						val associatedImplementation: FunctionImplementation? = null): SemanticModel(source, scope) {
	var original = this
	val root: FunctionSignature
		get() = superFunctionSignature?.root ?: original
	val isVariadic = associatedImplementation?.isVariadic ?: false
	val fixedParameterTypes: List<Type?>
	private val variadicParameterType: Type?
	val returnType = returnType ?: LiteralType(source, scope, SpecialType.NOTHING)
	var superFunctionSignature: FunctionSignature? = null
	var parentTypeDeclaration: TypeDeclaration? = null
		get() = if(this === original) field else original.parentTypeDeclaration
	private var llvmType: LlvmType? = null

	init {
		addSemanticModels(localTypeParameters, parameterTypes, whereClauseConditions)
		addSemanticModels(this.returnType)
		if(isVariadic) {
			fixedParameterTypes = parameterTypes.subList(0, parameterTypes.size - 1)
			variadicParameterType = parameterTypes.last()
		} else {
			fixedParameterTypes = parameterTypes
			variadicParameterType = null
		}
	}

	fun getLocalTypeSubstitutions(suppliedLocalTypes: List<Type>, suppliedValues: List<Value>): Map<TypeDeclaration, Type>? {
		if(suppliedLocalTypes.size > localTypeParameters.size)
			return null
		if(isVariadic) {
			if(suppliedValues.size < fixedParameterTypes.size)
				return null
		} else {
			if(suppliedValues.size != fixedParameterTypes.size)
				return null
		}
		val localTypeSubstitutions = HashMap<TypeDeclaration, Type>()
		for(parameterIndex in localTypeParameters.indices) {
			val localTypeParameter = localTypeParameters[parameterIndex]
			val requiredType = localTypeParameter.getLinkedSuperType()
			val suppliedType = suppliedLocalTypes.getOrNull(parameterIndex)
				?: inferTypeParameter(localTypeParameter, suppliedValues)
				?: return null
			if(requiredType?.accepts(suppliedType) == false)
				return null
			localTypeSubstitutions[localTypeParameter] = suppliedType
		}
		return localTypeSubstitutions
	}

	private fun inferTypeParameter(typeParameter: TypeDeclaration, suppliedValues: List<Value>): Type? {
		assert(suppliedValues.size >= fixedParameterTypes.size)

		val inferredTypes = LinkedList<Type>()
		for(parameterIndex in suppliedValues.indices) {
			val parameterType = getParameterTypeAt(parameterIndex)
			val suppliedType = suppliedValues[parameterIndex].providedType ?: continue
			parameterType?.inferTypeParameter(typeParameter, suppliedType, inferredTypes)
		}
		if(inferredTypes.isEmpty())
			return null
		return inferredTypes.combineOrUnion(this)
	}

	fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): FunctionSignature {
		val specificLocalTypeParameters = LinkedList<GenericTypeDeclaration>()
		for(localTypeParameter in localTypeParameters)
			specificLocalTypeParameters.add(localTypeParameter.withTypeSubstitutions(typeSubstitutions))
		val specificParametersTypes = LinkedList<Type?>()
		for(parameterType in parameterTypes)
			specificParametersTypes.add(parameterType?.withTypeSubstitutions(typeSubstitutions))
		val specificWhereClauseConditions = LinkedList<WhereClauseCondition>()
		for(whereClauseCondition in whereClauseConditions)
			specificWhereClauseConditions.add(whereClauseCondition.withTypeSubstitutions(typeSubstitutions))
		val specificSignature = FunctionSignature(source, scope, specificLocalTypeParameters, specificParametersTypes,
			returnType.withTypeSubstitutions(typeSubstitutions), specificWhereClauseConditions, associatedImplementation)
		specificSignature.original = original
		//TODO superFunctionSignature might not be set yet
		specificSignature.superFunctionSignature = superFunctionSignature?.withTypeSubstitutions(typeSubstitutions)
		return specificSignature
	}

	fun getMatch(localTypeSubstitutions: Map<TypeDeclaration, Type>, suppliedValues: List<Value>): FunctionType.Match? {
		assert(suppliedValues.size >= fixedParameterTypes.size)

		val conversions = HashMap<Value, InitializerDefinition>()
		var numberLiteralTypeScore = 0
		for(parameterIndex in suppliedValues.indices) {
			val parameterType = getParameterTypeAt(parameterIndex)?.withTypeSubstitutions(localTypeSubstitutions) ?: return null
			val suppliedValue = suppliedValues[parameterIndex]
			if(suppliedValue.isAssignableTo(parameterType)) {
				if(suppliedValue is NumberLiteral) {
					val effectiveParameterTypeDefinition = when(val effectiveParameterType = parameterType.effectiveType) {
						is SelfType -> effectiveParameterType.typeDeclaration
						is ObjectType -> effectiveParameterType.getTypeDeclaration()
						else -> null
					}
					if(effectiveParameterTypeDefinition != null) {
						if(SpecialType.INTEGER.matches(effectiveParameterTypeDefinition))
							numberLiteralTypeScore += 1
						else if(SpecialType.FLOAT.matches(effectiveParameterTypeDefinition))
							numberLiteralTypeScore += 2
					}
				}
			} else {
				val suppliedType = suppliedValue.providedType ?: return null
				val possibleConversions = parameterType.getConversionsFrom(suppliedType)
				if(possibleConversions.isEmpty())
					return null
				if(possibleConversions.size > 1) {
					context.addIssue(ConversionAmbiguity(source, suppliedType, parameterType, possibleConversions))
					return null
				}
				conversions[suppliedValue] = possibleConversions.first()
			}
		}
		return FunctionType.Match(this, localTypeSubstitutions, conversions, numberLiteralTypeScore)
	}

	fun fulfillsInheritanceRequirementsOf(superSignature: FunctionSignature): Boolean {
		if(parameterTypes.size != superSignature.parameterTypes.size)
			return false
		for(parameterIndex in parameterTypes.indices) {
			val parameterType = parameterTypes[parameterIndex] ?: continue
			val otherParameterType = superSignature.parameterTypes[parameterIndex] ?: continue
			if(!otherParameterType.accepts(parameterType))
				return false
		}
		return true
	}

	fun overrides(otherSignature: FunctionSignature): Boolean {
		if(superFunctionSignature == otherSignature)
			return true
		return superFunctionSignature?.overrides(otherSignature) ?: false
	}

	fun compareSpecificity(otherSignature: FunctionSignature): ComparisonResult {
		for(parameterIndex in 0 until max(fixedParameterTypes.size, otherSignature.fixedParameterTypes.size)) {
			val parameterType = getParameterTypeAt(parameterIndex) ?: continue
			val otherParameterType = otherSignature.getParameterTypeAt(parameterIndex) ?: continue
			if(parameterType != otherParameterType) {
				if(otherParameterType.accepts(parameterType)) return ComparisonResult.HIGHER
				if(parameterType.accepts(otherParameterType)) return ComparisonResult.LOWER
			}
		}
		val otherVariadicParameterType = otherSignature.variadicParameterType
		if(otherVariadicParameterType == null) {
			if(variadicParameterType != null)
				return ComparisonResult.LOWER
		} else {
			if(variadicParameterType == null)
				return ComparisonResult.HIGHER
			if(variadicParameterType != otherVariadicParameterType) {
				if(otherVariadicParameterType.accepts(variadicParameterType)) return ComparisonResult.HIGHER
				if(variadicParameterType.accepts(otherVariadicParameterType)) return ComparisonResult.LOWER
			}
		}
		return ComparisonResult.SAME
	}

	fun hasSameParameterTypesAs(otherSignature: FunctionSignature): Boolean {
		if(parameterTypes.size != otherSignature.parameterTypes.size)
			return false
		for(parameterIndex in parameterTypes.indices)
			if(parameterTypes[parameterIndex] != otherSignature.parameterTypes[parameterIndex])
				return false
		return true
	}

	fun getMatch(other: FunctionSignature): Boolean {
		if(other.parameterTypes.size != parameterTypes.size)
			return false
		for(parameterIndex in parameterTypes.indices) {
			val parameterType = parameterTypes[parameterIndex] ?: continue
			val otherParameterType = other.parameterTypes[parameterIndex] ?: continue
			if(!otherParameterType.accepts(parameterType))
				return false
		}
		return returnType.accepts(other.returnType)
	}

	fun getParameterTypeAt(index: Int): Type? {
		//TODO this should also work for lists, maps, etc. (see OverGenerator)
		return if(index < fixedParameterTypes.size)
			fixedParameterTypes[index]
		else
			(variadicParameterType as? PluralType)?.baseType
	}

	fun requiresParameters() = localTypeParameters.isNotEmpty() || fixedParameterTypes.isNotEmpty()

	override fun validate() {
		super.validate()
		if(this == original && associatedImplementation == null)
			scope.validate()
	}

	fun getLlvmType(constructor: LlvmConstructor): LlvmType {
		var llvmType = llvmType
		if(llvmType == null) {
			llvmType = buildLlvmType(constructor)
			this.llvmType = llvmType
		}
		return llvmType
	}

	fun buildLlvmType(constructor: LlvmConstructor): LlvmType {
		val parameterTypes = LinkedList<LlvmType?>()
		for(parameterIndex in this.parameterTypes.indices)
			parameterTypes.add(getEffectiveParameterType(parameterIndex)?.getLlvmType(constructor))
		val parentTypeDeclaration = parentTypeDeclaration
		parameterTypes.add(Context.EXCEPTION_PARAMETER_INDEX, constructor.pointerType)
		if(parentTypeDeclaration != null) {
			val implicitSelfType = if(parentTypeDeclaration == context.primitiveCompilationTarget)
				constructor.pointerType
			else
				parentTypeDeclaration.getLlvmReferenceType(constructor)
			parameterTypes.add(Context.THIS_PARAMETER_INDEX, implicitSelfType)
		}
		return constructor.buildFunctionType(parameterTypes, getEffectiveReturnType().getLlvmType(constructor), isVariadic)
	}

	fun getEffectiveParameterType(index: Int): Type? {
		return root.parameterTypes[index]?.effectiveType
	}

	fun getEffectiveReturnType(): Type {
		return root.returnType
	}

	override fun equals(other: Any?): Boolean {
		if(other !is FunctionSignature)
			return false
		if(returnType != other.returnType)
			return false
		if(!hasSameParameterTypesAs(other))
			return false
		return true
	}

	override fun hashCode(): Int {
		var result = localTypeParameters.hashCode()
		result = 31 * result + parameterTypes.hashCode()
		result = 31 * result + returnType.hashCode()
		return result
	}

	fun getIdentifier(name: String): String {
		return superFunctionSignature?.getIdentifier(name) ?: "$name${original.toString(false)}"
	}

	fun getIdentifier(kind: Operator.Kind): String {
		return superFunctionSignature?.getIdentifier(kind) ?: original.toString(false, kind)
	}

	override fun toString(): String {
		return toString(true)
	}

	fun toString(useLambdaStyleForFunctions: Boolean, kind: Operator.Kind? = null): String {
		return when(kind) {
			Operator.Kind.BRACKETS_GET -> {
				var stringRepresentation = "["
				if(localTypeParameters.isNotEmpty()) {
					stringRepresentation += localTypeParameters.joinToString()
					stringRepresentation += ";"
					if(parameterTypes.isNotEmpty())
						stringRepresentation += " "
				}
				stringRepresentation += "${parameterTypes.joinToString()}]"
				if(!SpecialType.NOTHING.matches(returnType))
					stringRepresentation += ": $returnType"
				stringRepresentation
			}
			Operator.Kind.BRACKETS_SET -> {
				var stringRepresentation = "["
				if(localTypeParameters.isNotEmpty()) {
					stringRepresentation += localTypeParameters.joinToString()
					stringRepresentation += ";"
					if(parameterTypes.size > 1)
						stringRepresentation += " "
				}
				for(typeIndex in 0 until parameterTypes.size - 1) {
					if(typeIndex != 0)
						stringRepresentation += ", "
					stringRepresentation += parameterTypes[typeIndex]
				}
				stringRepresentation += "]"
				stringRepresentation += "(${parameterTypes.lastOrNull()})"
				if(!SpecialType.NOTHING.matches(returnType))
					stringRepresentation += ": $returnType"
				stringRepresentation
			}
			null -> {
				var stringRepresentation = ""
				if(!useLambdaStyleForFunctions || localTypeParameters.isNotEmpty() || parameterTypes.isNotEmpty()) {
					stringRepresentation += "("
					if(localTypeParameters.isNotEmpty()) {
						stringRepresentation += localTypeParameters.joinToString()
						stringRepresentation += ";"
						if(parameterTypes.isNotEmpty())
							stringRepresentation += " "
					}
					stringRepresentation += "${parameterTypes.joinToString()})"
				}
				if(useLambdaStyleForFunctions) {
					if(stringRepresentation.isNotEmpty())
						stringRepresentation += " "
					stringRepresentation += "=>"
					stringRepresentation += if(SpecialType.NOTHING.matches(returnType)) "|" else " $returnType"
				} else {
					if(!SpecialType.NOTHING.matches(returnType))
						stringRepresentation += ": $returnType"
				}
				stringRepresentation
			}
			else -> {
				if(kind.isUnary && !(kind.isBinary && parameterTypes.size == 1)) {
					var stringRepresentation = kind.stringRepresentation
					if(kind.returnsValue)
						stringRepresentation += ": $returnType"
					stringRepresentation
				} else if(kind.isBinary) {
					var stringRepresentation = " ${kind.stringRepresentation} ${parameterTypes.firstOrNull()}"
					if(kind.returnsValue)
						stringRepresentation += ": $returnType"
					stringRepresentation
				} else {
					throw CompilerError(source, "Operator of kind '${kind.name}' is neither unary nor binary.")
				}
			}
		}
	}
}
