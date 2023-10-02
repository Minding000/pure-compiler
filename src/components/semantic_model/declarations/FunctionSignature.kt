package components.semantic_model.declarations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.semantic_model.context.SpecialType
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.BlockScope
import components.semantic_model.types.LiteralType
import components.semantic_model.types.PluralType
import components.semantic_model.types.Type
import components.semantic_model.values.Operator
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import util.combine
import java.util.*
import kotlin.math.max

class FunctionSignature(override val source: SyntaxTreeNode, override val scope: BlockScope,
						val localTypeParameters: List<GenericTypeDeclaration>, val parameterTypes: List<Type?>,
						returnType: Type?, val associatedImplementation: FunctionImplementation? = null): SemanticModel(source, scope) {
	var original = this
	val isVariadic = associatedImplementation?.isVariadic ?: false
	val fixedParameterTypes: List<Type?>
	private val variadicParameterType: Type?
	val returnType = returnType ?: LiteralType(source, scope, SpecialType.NOTHING)
	var superFunctionSignature: FunctionSignature? = null
	var parentDefinition: TypeDeclaration? = null
	private var llvmType: LlvmType? = null

	init {
		addSemanticModels(localTypeParameters, parameterTypes)
		addSemanticModels(this.returnType)
		if(isVariadic) {
			this.fixedParameterTypes = parameterTypes.subList(0, parameterTypes.size - 1)
			this.variadicParameterType = parameterTypes.last()
		} else {
			this.fixedParameterTypes = parameterTypes
			this.variadicParameterType = null
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
			val suppliedType = suppliedValues[parameterIndex].type ?: continue
			parameterType?.inferTypeParameter(typeParameter, suppliedType, inferredTypes)
		}
		if(inferredTypes.isEmpty())
			return null
		return inferredTypes.combine(this)
	}

	fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): FunctionSignature {
		val specificLocalTypeParameters = LinkedList<GenericTypeDeclaration>()
		for(localTypeParameter in localTypeParameters)
			specificLocalTypeParameters.add(localTypeParameter.withTypeSubstitutions(typeSubstitutions))
		val specificParametersTypes = LinkedList<Type?>()
		for(parameterType in parameterTypes)
			specificParametersTypes.add(parameterType?.withTypeSubstitutions(typeSubstitutions))
		val specificSignature = FunctionSignature(source, scope, specificLocalTypeParameters, specificParametersTypes,
			returnType.withTypeSubstitutions(typeSubstitutions), associatedImplementation)
		specificSignature.original = this
		specificSignature.superFunctionSignature = superFunctionSignature
		return specificSignature
	}

	fun accepts(localTypeSubstitutions: Map<TypeDeclaration, Type>, suppliedValues: List<Value>): Boolean {
		assert(suppliedValues.size >= fixedParameterTypes.size)

		for(parameterIndex in suppliedValues.indices) {
			val parameterType = getParameterTypeAt(parameterIndex)?.withTypeSubstitutions(localTypeSubstitutions)
			if(!suppliedValues[parameterIndex].isAssignableTo(parameterType))
				return false
		}
		return true
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

	fun isMoreSpecificThan(otherSignature: FunctionSignature): Boolean {
		for(parameterIndex in 0 until max(fixedParameterTypes.size, otherSignature.fixedParameterTypes.size)) {
			val parameterType = getParameterTypeAt(parameterIndex) ?: return false
			val otherParameterType = otherSignature.getParameterTypeAt(parameterIndex) ?: return true
			if(parameterType != otherParameterType)
				return otherParameterType.accepts(parameterType)
		}
		val otherVariadicParameterType = otherSignature.variadicParameterType
		if(otherVariadicParameterType != null) {
			if(variadicParameterType == null)
				return true
			if(variadicParameterType != otherVariadicParameterType)
				return otherVariadicParameterType.accepts(variadicParameterType)
		}
		return false
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
			val parameterTypes = LinkedList<LlvmType?>(parameterTypes.map { parameterType -> parameterType?.getLlvmType(constructor) })
			val parentDefinition = parentDefinition
			if(parentDefinition != null)
				parameterTypes.addFirst(constructor.pointerType)
			parameterTypes.addFirst(constructor.pointerType)
			llvmType = constructor.buildFunctionType(parameterTypes, returnType.getLlvmType(constructor), isVariadic)
			this.llvmType = llvmType
		}
		return llvmType
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
