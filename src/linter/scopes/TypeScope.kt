package linter.scopes

import linter.Linter
import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.InitializerDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.literals.SimpleType
import linter.elements.values.TypeDefinition
import linter.elements.values.Value
import linter.elements.values.VariableValueDeclaration
import linter.messages.Message
import java.util.*
import kotlin.collections.HashMap

class TypeScope(private val parentScope: MutableScope, private val superScope: InterfaceScope?): MutableScope() {
	var instanceConstant: VariableValueDeclaration? = null
	var typeDefinition: TypeDefinition? = null
	private val declaredTypes = HashMap<String, TypeDefinition>()
	private val declaredValues = HashMap<String, VariableValueDeclaration>()
	private val initializers = LinkedList<InitializerDefinition>()
	private val functions = HashMap<String, HashMap<String, FunctionDefinition>>()
	private val operators = HashMap<String, HashMap<String, OperatorDefinition>>()

	companion object {
		const val SELF_REFERENCE = "this"
	}

	fun createInstanceConstant(definition: TypeDefinition) {
		instanceConstant = VariableValueDeclaration(definition.source, SELF_REFERENCE, SimpleType(definition), true)
	}

	override fun declareInitializer(linter: Linter, initializer: InitializerDefinition) {
		var previousDeclaration: InitializerDefinition? = null
		initializerIteration@for(declaredInitializer in initializers) {
			if(declaredInitializer.parameters.size != initializer.parameters.size)
				continue
			for(i in initializer.parameters.indices) {
				if(declaredInitializer.parameters[i].type != initializer.parameters[i].type)
					continue@initializerIteration
			}
			previousDeclaration = declaredInitializer
			break
		}
		if(previousDeclaration == null) {
			initializers.add(initializer)
			linter.messages.add(Message(
				"${initializer.source.getStartString()}: Declaration of initializer '${typeDefinition?.name}(${initializer.variation})'.", Message.Type.DEBUG))
		} else {
			linter.messages.add(Message(
				"${initializer.source.getStartString()}: Redeclaration of initializer '${typeDefinition?.name}(${initializer.variation})'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
		}
	}

	override fun declareType(linter: Linter, type: TypeDefinition) {
		var previousDeclaration = parentScope.resolveType(type.name)
		if(previousDeclaration != null) {
			linter.messages.add(Message(
				"${type.source.getStartString()}: '${type.name}' shadows a type.", Message.Type.WARNING))
		}
		previousDeclaration = superScope?.resolveType(type.name)
		if(previousDeclaration != null) {
			linter.messages.add(Message(
				"${type.source.getStartString()}: Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}." +
						" Use the 'override' keyword to modify it.", Message.Type.ERROR))
			return
		}
		previousDeclaration = declaredTypes.putIfAbsent(type.name, type)
		if(previousDeclaration == null)
			linter.messages.add(Message(
				"${type.source.getStartString()}: Declaration of type '${type.name}'.", Message.Type.DEBUG))
		else
			linter.messages.add(Message(
				"${type.source.getStartString()}: Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	fun resolveInitializer(suppliedValues: List<Value>): InitializerDefinition? {
		initializerIteration@for(initializer in initializers) {
			if(initializer.parameters.size != suppliedValues.size)
				continue
			for(i in suppliedValues.indices) {
				if(suppliedValues[i].type?.let { initializer.parameters[i].type?.accepts(it) } == false)
					continue@initializerIteration
			}
			return initializer
		}
		return null
	}

	override fun resolveType(name: String): TypeDefinition? {
		return declaredTypes[name]
			?: superScope?.resolveType(name)
			?: parentScope.resolveType(name)
	}

	override fun declareValue(linter: Linter, value: VariableValueDeclaration) {
		var previousDeclaration = parentScope.resolveReference(value.name)
		if(previousDeclaration != null) {
			linter.messages.add(Message(
				"${value.source.getStartString()}: '${value.name}' shadows a variable.", Message.Type.WARNING))
		}
		previousDeclaration = superScope?.resolveReference(value.name)
		if(previousDeclaration != null) {
			linter.messages.add(Message(
				"${value.source.getStartString()}: Redeclaration of type '${value.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}." +
						" Use the 'override' keyword to modify it.", Message.Type.ERROR))
			return
		}
		previousDeclaration = declaredValues.putIfAbsent(value.name, value)
		if(previousDeclaration == null)
			linter.messages.add(Message(
				"${value.source.getStartString()}: Declaration of value '${value.name}'.", Message.Type.DEBUG))
		else
			linter.messages.add(Message(
			"${value.source.getStartString()}: Redeclaration of value '${value.name}'," +
					" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun resolveReference(name: String): VariableValueDeclaration? {
		if(name == SELF_REFERENCE)
			return instanceConstant
		return declaredValues[name]
			?: superScope?.resolveReference(name)
			?: parentScope.resolveReference(name)
	}

	override fun declareFunction(linter: Linter, function: FunctionDefinition) {
		val signatures = functions.getOrPut(function.name) { HashMap() }
		val previousDeclaration = signatures.putIfAbsent(function.variation, function)
		if(previousDeclaration == null)
			linter.messages.add(Message(
				"${function.source.getStartString()}: Declaration of function '$function'.", Message.Type.DEBUG))
		else
			linter.messages.add(Message(
				"${function.source.getStartString()}: Redeclaration of function '$function'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun declareOperator(linter: Linter, operator: OperatorDefinition) {
		val signatures = operators.getOrPut(operator.name) { HashMap() }
		val previousDeclaration = signatures.putIfAbsent(operator.variation, operator)
		if(previousDeclaration == null)
			linter.messages.add(Message(
				"${operator.source.getStartString()}: Declaration of operator '$operator'.", Message.Type.DEBUG))
		else
			linter.messages.add(Message(
			"${operator.source.getStartString()}: Redeclaration of operator '$operator'," +
					" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun resolveFunction(name: String, variation: String): FunctionDefinition? {
		return functions[name]?.get(variation)
			?: superScope?.resolveFunction(name, variation)
			?: parentScope.resolveFunction(name, variation)
	}

	override fun resolveOperator(name: String, variation: String): OperatorDefinition? {
		return operators[name]?.get(variation)
			?: superScope?.resolveOperator(name, variation)
			?: parentScope.resolveOperator(name, variation)
	}
}