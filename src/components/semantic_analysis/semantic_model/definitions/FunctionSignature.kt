package components.semantic_analysis.semantic_model.definitions

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.semantic_analysis.semantic_model.types.PluralType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Operator
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import logger.issues.definition.InvalidVariadicParameterPosition
import logger.issues.definition.MultipleVariadicParameters
import util.combine
import java.util.*

class FunctionSignature(override val source: SyntaxTreeNode, override val scope: BlockScope, val genericParameters: List<TypeDefinition>,
						val parameterTypes: List<Type?>, returnType: Type?): SemanticModel(source, scope) {
	val fixedParameterTypes: List<Type?>
	val variadicParameterType: PluralType?
	val returnType = returnType ?: LiteralType(source, scope, SpecialType.NOTHING)
	var superFunctionSignature: FunctionSignature? = null
	var parentDefinition: TypeDefinition? = null
	private var llvmType: LlvmType? = null

	init {
		addSemanticModels(genericParameters, parameterTypes)
		addSemanticModels(this.returnType)

		val fixedParameterTypes = LinkedList<Type?>()
		var variadicParameterType: PluralType? = null
		for(parameterIndex in parameterTypes.indices) {
			val parameterType = parameterTypes[parameterIndex]
			if(parameterIndex == parameterTypes.size - 1) {
				if(parameterType is PluralType) {
					variadicParameterType = parameterType
					break
				}
			}
			fixedParameterTypes.add(parameterType)
		}
		this.fixedParameterTypes = fixedParameterTypes
		this.variadicParameterType = variadicParameterType
	}

	fun getTypeSubstitutions(suppliedTypes: List<Type>, suppliedValues: List<Value>): Map<TypeDefinition, Type>? {
		if(suppliedTypes.size > genericParameters.size)
			return null
		if(variadicParameterType == null) {
			if(suppliedValues.size != fixedParameterTypes.size)
				return null
		} else {
			if(suppliedValues.size < fixedParameterTypes.size)
				return null
		}
		val typeSubstitutions = HashMap<TypeDefinition, Type>()
		for(parameterIndex in genericParameters.indices) {
			val genericParameter = genericParameters[parameterIndex]
			val requiredType = genericParameter.getLinkedSuperType()
			val suppliedType = suppliedTypes.getOrNull(parameterIndex)
				?: inferTypeParameter(genericParameter, suppliedValues)
				?: return null
			if(requiredType?.accepts(suppliedType) == false)
				return null
			typeSubstitutions[genericParameter] = suppliedType
		}
		return typeSubstitutions
	}

	private fun inferTypeParameter(typeParameter: TypeDefinition, suppliedValues: List<Value>): Type? {
		assert(suppliedValues.size >= fixedParameterTypes.size)

		val inferredTypes = LinkedList<Type>()
		for(parameterIndex in suppliedValues.indices) {
			val parameterType = getParameterTypeAt(parameterIndex)
			val suppliedType = suppliedValues[parameterIndex].type ?: continue
			parameterType?.inferType(typeParameter, suppliedType, inferredTypes)
		}
		if(inferredTypes.isEmpty())
			return null
		return inferredTypes.combine(this)
	}

	fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): FunctionSignature {
		val specificGenericParameters = LinkedList<TypeDefinition>()
		for(genericParameter in genericParameters) {
			genericParameter.withTypeSubstitutions(typeSubstitution) { specificDefinition ->
				specificGenericParameters.add(specificDefinition)
			}
		}
		val specificParametersTypes = LinkedList<Type?>()
		for(parameterType in parameterTypes)
			specificParametersTypes.add(parameterType?.withTypeSubstitutions(typeSubstitution))
		return FunctionSignature(source, scope, specificGenericParameters, specificParametersTypes,
			returnType.withTypeSubstitutions(typeSubstitution))
	}

	fun accepts(suppliedValues: List<Value>): Boolean {
		assert(suppliedValues.size >= fixedParameterTypes.size)

		for(parameterIndex in suppliedValues.indices) {
			val parameterType = getParameterTypeAt(parameterIndex)
			if(!suppliedValues[parameterIndex].isAssignableTo(parameterType))
				return false
		}
		return true
	}

	fun isMoreSpecificThan(otherSignature: FunctionSignature): Boolean { //TODO mind variadic parameters here and in functions below (write tests!)
		if(otherSignature.parameterTypes.size != parameterTypes.size)
			return false
		var areSignaturesEqual = true
		for(parameterIndex in parameterTypes.indices) {
			val parameterType = parameterTypes[parameterIndex] ?: return false
			val otherParameterType = otherSignature.parameterTypes[parameterIndex]
			if(otherParameterType == null) {
				areSignaturesEqual = false
				continue
			}
			if(otherParameterType != parameterType) {
				areSignaturesEqual = false
				if(!otherParameterType.accepts(parameterType))
					return false
			}
		}
		return !areSignaturesEqual
	}

	fun fulfillsInheritanceRequirementsOf(superSignature: FunctionSignature): Boolean {
		if(!returnType.isAssignableTo(superSignature.returnType))
			return false
		if(parameterTypes.size != superSignature.parameterTypes.size)
			return false
		for(parameterIndex in parameterTypes.indices) {
			val superParameterType = superSignature.parameterTypes[parameterIndex] ?: continue
			val baseParameterType = parameterTypes[parameterIndex] ?: continue
			if(!baseParameterType.accepts(superParameterType))
				return false
		}
		return true
	}

	fun hasSameParameterTypesAs(otherSignature: FunctionSignature): Boolean {
		if(parameterTypes.size != otherSignature.parameterTypes.size)
			return false
		for(parameterIndex in parameterTypes.indices)
			if(parameterTypes[parameterIndex] != otherSignature.parameterTypes[parameterIndex])
				return false
		return true
	}

	fun accepts(other: FunctionSignature): Boolean {
		if(other.parameterTypes.size != parameterTypes.size)
			return false
		for(parameterIndex in parameterTypes.indices) {
			if(parameterTypes[parameterIndex]?.let { parameterType ->
					other.parameterTypes[parameterIndex]?.accepts(parameterType) } == false)
				return false
		}
		if(!returnType.accepts(other.returnType))
			return false
		return true
	}

	override fun validate() {
		super.validate()
		validateVariadicParameter()
	}

	private fun validateVariadicParameter() {
		for(parameterType in fixedParameterTypes) {
			if(parameterType is PluralType) {
				if(variadicParameterType == null)
					context.addIssue(InvalidVariadicParameterPosition(parameterType.source))
				else
					context.addIssue(MultipleVariadicParameters(source))
			}
		}
	}

	fun getComputedReturnType(): Type {
		returnType.determineTypes()
		return returnType
	}

	fun getParameterTypeAt(index: Int): Type? {
		return if(index < fixedParameterTypes.size)
			fixedParameterTypes[index]
		else
			variadicParameterType?.baseType
	}

	fun requiresParameters() = genericParameters.isNotEmpty() || fixedParameterTypes.isNotEmpty()

	override fun equals(other: Any?): Boolean {
		if(other !is FunctionSignature)
			return false
		if(returnType != other.returnType)
			return false
		if(!this.hasSameParameterTypesAs(other))
			return false
		return true
	}

	override fun hashCode(): Int {
		var result = genericParameters.hashCode()
		result = 31 * result + parameterTypes.hashCode()
		result = 31 * result + returnType.hashCode()
		return result
	}

	override fun toString(): String {
		return toString(true)
	}

	fun toString(useLambdaStyleForFunctions: Boolean, kind: Operator.Kind? = null): String {
		return when(kind) {
			Operator.Kind.BRACKETS_GET -> {
				var stringRepresentation = "["
				if(genericParameters.isNotEmpty()) {
					stringRepresentation += genericParameters.joinToString()
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
				if(genericParameters.isNotEmpty()) {
					stringRepresentation += genericParameters.joinToString()
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
				if(!useLambdaStyleForFunctions || genericParameters.isNotEmpty() || parameterTypes.isNotEmpty()) {
					stringRepresentation += "("
					if(genericParameters.isNotEmpty()) {
						stringRepresentation += genericParameters.joinToString()
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

	fun getLlvmType(constructor: LlvmConstructor): LlvmType {
		var llvmType = llvmType
		if(llvmType == null) {
			val parameterTypes = LinkedList<LlvmType?>(fixedParameterTypes.map { parameterType -> parameterType?.getLlvmType(constructor) })
			val parentDefinition = parentDefinition
			if(parentDefinition != null)
				parameterTypes.addFirst(constructor.createPointerType(parentDefinition.llvmType))
			llvmType = constructor.buildFunctionType(parameterTypes, returnType.getLlvmType(constructor), variadicParameterType != null)
			this.llvmType = llvmType
		}
		return llvmType
	}
}
