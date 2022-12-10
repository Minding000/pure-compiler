package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.semantic_model.definitions.*
import errors.user.SignatureResolutionAmbiguityError
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Instance
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import java.util.*

class InterfaceScope(private val type: Type): Scope() {
	private val types = HashMap<String, TypeDefinition>()
	private val values = HashMap<String, InterfaceMember>()
	private val initializers = LinkedList<InitializerDefinition>()
	private val operators = LinkedList<OperatorDefinition>()

	fun hasType(type: TypeDefinition): Boolean = types.containsValue(type)

	fun hasValue(value: InterfaceMember): Boolean = values.containsValue(value)

	fun hasInstance(name: String): Boolean {
		for((_, value) in values) {
			if(value is Instance && value.name == name)
				return true
		}
		return false
	}

	fun hasOperator(operator: OperatorDefinition): Boolean = operators.contains(operator)

	override fun subscribe(type: Type) {
		super.subscribe(type)
		for((_, typeDefinition) in types)
			type.onNewType(typeDefinition)
		for((_, value) in values)
			type.onNewValue(value)
		for(initializer in initializers)
			type.onNewInitializer(initializer)
		for(operator in operators)
			type.onNewOperator(operator)
	}

	fun addType(type: TypeDefinition) {
		if(!types.containsKey(type.name)) {
			types[type.name] = type
			onNewType(type)
		}
	}

	fun addValue(value: InterfaceMember) {
		if(!values.containsKey(value.name)) {
			values[value.name] = value
			onNewValue(value)
		}
	}

	fun addInitializer(initializer: InitializerDefinition) {
		initializers.add(initializer)
		onNewInitializer(initializer)
	}

	fun addOperator(operator: OperatorDefinition) {
		operators.add(operator)
		onNewOperator(operator)
	}

	override fun resolveValue(name: String): InterfaceMember? {
		return values[name]
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name]
	}

	fun resolveInitializer(suppliedValues: List<Value>): MatchResult? =
		resolveInitializer(listOf(), listOf(), listOf(), suppliedValues)

	fun resolveInitializer(genericDefinitionTypes: List<TypeDefinition>, suppliedDefinitionTypes: List<Type>,
						   suppliedTypes: List<Type>, suppliedValues: List<Value>): MatchResult? {
		val matches = getMatchingInitializers(genericDefinitionTypes, suppliedDefinitionTypes, suppliedTypes,
			suppliedValues)
		if(matches.isEmpty())
			return null
		specificityPrecedenceLoop@for(match in matches) {
			for(otherMatch in matches) {
				if(otherMatch == match)
					continue
				if(!match.signature.isMoreSpecificThan(otherMatch.signature))
					continue@specificityPrecedenceLoop
			}
			for(parameterIndex in suppliedValues.indices)
				suppliedValues[parameterIndex].setInferredType(match.signature.parameters[parameterIndex].type)
			return match
		}
		throw SignatureResolutionAmbiguityError(matches.map { match -> match.signature })
	}

	private fun getMatchingInitializers(genericDefinitionTypes: List<TypeDefinition>,
										suppliedDefinitionTypes: List<Type>, suppliedTypes: List<Type>,
										suppliedValues: List<Value>): List<MatchResult> {
		val validSignatures = LinkedList<MatchResult>()
		for(signature in initializers) {
			var specificSignature = signature
			val definitionTypeSubstitutions = signature.getDefinitionTypeSubstitutions(genericDefinitionTypes,
				suppliedDefinitionTypes, suppliedValues) ?: continue
			if(definitionTypeSubstitutions.isNotEmpty())
				specificSignature = specificSignature.withTypeSubstitutions(definitionTypeSubstitutions) //TODO the copied unit should be added to units (same for functions and operators)
			val typeSubstitutions = specificSignature.getTypeSubstitutions(suppliedTypes, suppliedValues) ?: continue
			if(typeSubstitutions.isNotEmpty())
				specificSignature = specificSignature.withTypeSubstitutions(typeSubstitutions) //TODO the copied unit should be added to units (same for functions and operators)
			if(specificSignature.accepts(suppliedValues))
				validSignatures.add(MatchResult(specificSignature, definitionTypeSubstitutions))
		}
		return validSignatures
	}

	class MatchResult(val signature: InitializerDefinition, val definitionTypeSubstitutions: Map<TypeDefinition, Type>)

	override fun resolveOperator(kind: OperatorDefinition.Kind, suppliedValues: List<Value>):
			OperatorDefinition? {
		val validSignatures = getMatchingOperators(kind, suppliedValues)
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
				suppliedValues[parameterIndex].setInferredType(signature.valueParameters[parameterIndex].type)
			return signature
		}
		throw SignatureResolutionAmbiguityError(validSignatures)
	}

	private fun getMatchingOperators(kind: OperatorDefinition.Kind, suppliedValues: List<Value>): List<OperatorDefinition> {
		val validSignatures = LinkedList<OperatorDefinition>()
		for(signature in operators) {
			if(signature.kind != kind)
				continue
			if(signature.accepts(suppliedValues))
				validSignatures.add(signature)
		}
		//TODO check: should this be as complex as FunctionType.getMatchingSignatures()?
		// -> write tests to find out -> yes!
		return validSignatures
	}

	override fun resolveIndexOperator(suppliedTypes: List<Type>, suppliedIndexValues: List<Value>,
									  suppliedParameterValues: List<Value>): IndexOperatorDefinition? {
		val validSignatures = getMatchingIndexOperators(suppliedTypes, suppliedIndexValues, suppliedParameterValues)
		if(validSignatures.isEmpty())
			return null
		specificityPrecedenceLoop@for(signature in validSignatures) {
			for(otherSignature in validSignatures) {
				if(otherSignature === signature)
					continue
				if(!signature.isMoreSpecificThan(otherSignature))
					continue@specificityPrecedenceLoop
			}
			for(indexIndex in suppliedIndexValues.indices)
				suppliedIndexValues[indexIndex].setInferredType(signature.indexParameters[indexIndex].type)
			for(parameterIndex in suppliedParameterValues.indices)
				suppliedParameterValues[parameterIndex].setInferredType(signature.valueParameters[parameterIndex].type)
			return signature
		}
		throw SignatureResolutionAmbiguityError(validSignatures)
	}

	private fun getMatchingIndexOperators(suppliedTypes: List<Type>, suppliedIndexValues: List<Value>,
										  suppliedParameterValues: List<Value>): List<IndexOperatorDefinition> {
		val validSignatures = LinkedList<IndexOperatorDefinition>()
		for(signature in operators) {
			if(signature !is IndexOperatorDefinition)
				continue
			val typeSubstitutions = signature.getTypeSubstitutions(suppliedTypes, suppliedIndexValues,
				suppliedParameterValues) ?: continue
			val specificSignature = if(typeSubstitutions.isEmpty())
				signature
			else
				signature.withTypeSubstitutions(typeSubstitutions)
			if(specificSignature.accepts(suppliedIndexValues, suppliedParameterValues))
				validSignatures.add(specificSignature)
		}
		//TODO check: should this be as complex as FunctionType.getMatchingSignatures()?
		// -> write tests to find out -> yes!
		return validSignatures
	}

	fun getAbstractMembers(): List<MemberDeclaration> = type.getAbstractMembers()

	override fun toString(): String {
		return "InterfaceScope of $type"
	}
}
