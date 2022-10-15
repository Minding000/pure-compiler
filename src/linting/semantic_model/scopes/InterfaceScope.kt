package linting.semantic_model.scopes

import errors.user.SignatureResolutionAmbiguityError
import linting.semantic_model.definitions.*
import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.Type
import linting.semantic_model.values.Instance
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValueDeclaration
import java.util.*

class InterfaceScope(private val type: Type): Scope() {
	private val types = HashMap<String, TypeDefinition>()
	private val values = HashMap<String, VariableValueDeclaration>()
	private val initializers = LinkedList<InitializerDefinition>()
	private val operators = LinkedList<OperatorDefinition>()

	fun hasType(type: TypeDefinition): Boolean = types.containsValue(type)

	fun hasValue(value: VariableValueDeclaration): Boolean = values.containsValue(value)

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

	fun addValue(value: VariableValueDeclaration) {
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

	override fun resolveValue(name: String): VariableValueDeclaration? {
		return values[name]
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name]
	}

	fun resolveInitializer(suppliedValues: List<Value>): InitializerDefinition? {
		val validSignatures = getMatchingInitializers(suppliedValues)
		if(validSignatures.isEmpty())
			return null
		specificityPrecedenceLoop@for(signature in validSignatures) {
			for(otherSignature in validSignatures) {
				if(otherSignature == signature)
					continue
				if(!signature.isMoreSpecificThan(otherSignature))
					continue@specificityPrecedenceLoop
			}
			for(parameterIndex in suppliedValues.indices)
				suppliedValues[parameterIndex].setInferredType(signature.parameters[parameterIndex].type)
			return signature
		}
		throw SignatureResolutionAmbiguityError(validSignatures)
	}

	private fun getMatchingInitializers(suppliedValues: List<Value>): List<InitializerDefinition> {
		val validSignatures = LinkedList<InitializerDefinition>()
		for(signature in initializers) {
			if(signature.accepts(suppliedValues))
				validSignatures.add(signature)
		}
		return validSignatures
	}

	override fun resolveOperator(name: String, suppliedValues: List<Value>):
			OperatorDefinition? {
		val validSignatures = getMatchingOperators(name, suppliedValues)
		if(validSignatures.isEmpty())
			return null
		specificityPrecedenceLoop@for(signature in validSignatures) {
			for(otherSignature in validSignatures) {
				if(otherSignature == signature)
					continue
				if(!signature.isMoreSpecificThan(otherSignature))
					continue@specificityPrecedenceLoop
			}
			for(parameterIndex in suppliedValues.indices)
				suppliedValues[parameterIndex].setInferredType(signature.parameters[parameterIndex].type)
			return signature
		}
		throw SignatureResolutionAmbiguityError(validSignatures)
	}

	private fun getMatchingOperators(name: String, suppliedValues: List<Value>): List<OperatorDefinition> {
		val validSignatures = LinkedList<OperatorDefinition>()
		for(signature in operators) {
			if(signature.name != name)
				continue
			if(signature.accepts(suppliedValues))
				validSignatures.add(signature)
		} //TODO check: should this be as complex as FunctionType.getMatchingSignatures()?
		return validSignatures
	}

	override fun resolveIndexOperator(suppliedIndexValues: List<Value>, suppliedParameterValues: List<Value>):
			IndexOperatorDefinition? {
		val validSignatures = getMatchingIndexOperators(suppliedIndexValues, suppliedParameterValues)
		if(validSignatures.isEmpty())
			return null
		specificityPrecedenceLoop@for(signature in validSignatures) {
			for(otherSignature in validSignatures) {
				if(otherSignature == signature)
					continue
				if(!signature.isMoreSpecificThan(otherSignature))
					continue@specificityPrecedenceLoop
			}
			for(indexIndex in suppliedIndexValues.indices)
				suppliedIndexValues[indexIndex].setInferredType(signature.indices[indexIndex].type)
			for(parameterIndex in suppliedParameterValues.indices)
				suppliedParameterValues[parameterIndex].setInferredType(signature.parameters[parameterIndex].type)
			return signature
		}
		throw SignatureResolutionAmbiguityError(validSignatures)
	}

	private fun getMatchingIndexOperators(suppliedIndexValues: List<Value>, suppliedParameterValues: List<Value>):
		List<IndexOperatorDefinition> {
		val validSignatures = LinkedList<IndexOperatorDefinition>()
		for(signature in operators) {
			if(signature is IndexOperatorDefinition && signature.accepts(suppliedIndexValues, suppliedParameterValues))
				validSignatures.add(signature)
		} //TODO check: should this be as complex as FunctionType.getMatchingSignatures()?
		return validSignatures
	}

	fun getGenericTypes(): LinkedList<ObjectType> {
		val genericTypes = LinkedList<ObjectType>()
		for((_, typeDefinition) in types)
			if(typeDefinition is GenericTypeDefinition)
				genericTypes.add(ObjectType(typeDefinition))
		return genericTypes
	}

	override fun toString(): String {
		return "InterfaceScope of $type"
	}
}
