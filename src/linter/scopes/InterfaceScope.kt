package linter.scopes

import linter.elements.definitions.IndexOperatorDefinition
import linter.elements.definitions.InitializerDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.literals.FunctionType
import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import java.util.*
import kotlin.collections.HashMap

class InterfaceScope(private val type: Type): Scope() {
	private val types = HashMap<String, TypeDefinition>()
	private val values = HashMap<String, VariableValueDeclaration>()
	private val initializers = LinkedList<InitializerDefinition>()
	private val operators = LinkedList<OperatorDefinition>()
	val genericTypes = HashMap<Type, Type>()

	fun hasType(type: TypeDefinition): Boolean = types.containsValue(type)

	fun hasValue(value: VariableValueDeclaration): Boolean = values.containsValue(value)

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
		types[type.name] = type
		onNewType(type)
	}

	fun addValue(value: VariableValueDeclaration) {
		values[value.name] = value
		onNewValue(value)
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

	fun resolveInitializer(suppliedTypes: List<Type?>): InitializerDefinition? {
		val validSignatures = LinkedList<InitializerDefinition>()
		for(signature in initializers) {
			if(signature.accepts(suppliedTypes))
				validSignatures.add(signature)
		}
		if(validSignatures.isEmpty())
			return null
		signatureCheck@for(signature in validSignatures) {
			for(otherSignature in validSignatures) {
				if(otherSignature == signature)
					continue
				if(!otherSignature.accepts(signature.parameters.map { p -> p.type }))
					continue@signatureCheck
			}
			return signature
		}
		throw FunctionType.SignatureResolutionAmbiguityError(validSignatures)
	}

	override fun resolveOperator(name: String, suppliedTypes: List<Type?>):
			OperatorDefinition? {
		val validSignatures = LinkedList<OperatorDefinition>()
		for(signature in operators) {
			if(signature.accepts(suppliedTypes))
				validSignatures.add(signature)
		}
		if(validSignatures.isEmpty())
			return null
		signatureCheck@for(signature in validSignatures) {
			for(otherSignature in validSignatures) {
				if(otherSignature == signature)
					continue
				if(!otherSignature.accepts(signature.parameters.map { p -> p.type }))
					continue@signatureCheck
			}
			return signature
		}
		throw FunctionType.SignatureResolutionAmbiguityError(validSignatures)
	}

	override fun resolveIndexOperator(name: String, suppliedIndexTypes: List<Type?>, suppliedParameterTypes: List<Type?>):
			IndexOperatorDefinition? {
		val validSignatures = LinkedList<IndexOperatorDefinition>()
		for(signature in operators) {
			if(signature is IndexOperatorDefinition && signature.accepts(suppliedIndexTypes, suppliedParameterTypes))
				validSignatures.add(signature)
		}
		if(validSignatures.isEmpty())
			return null
		signatureCheck@for(signature in validSignatures) {
			for(otherSignature in validSignatures) {
				if(otherSignature == signature)
					continue
				if(!otherSignature.accepts(signature.indices.map { i -> i.type }, signature.parameters.map { p -> p.type }))
					continue@signatureCheck
			}
			return signature
		}
		throw FunctionType.SignatureResolutionAmbiguityError(validSignatures)
	}

	override fun toString(): String {
		return "InterfaceScope of $type"
	}
}