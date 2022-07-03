package linter.scopes

import linter.Linter
import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.IndexOperatorDefinition
import linter.elements.definitions.InitializerDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.literals.SimpleType
import linter.elements.literals.Type
import linter.elements.values.TypeDefinition
import linter.elements.values.Value
import linter.elements.values.VariableValueDeclaration
import linter.messages.Message
import java.util.*
import kotlin.collections.HashMap

class TypeScope(private val parentScope: MutableScope, private val superScope: InterfaceScope?): MutableScope() {
	var instanceConstant: VariableValueDeclaration? = null
	var typeDefinition: TypeDefinition? = null
	val types = HashMap<String, TypeDefinition>()
	val values = HashMap<String, VariableValueDeclaration>()
	val initializers = LinkedList<InitializerDefinition>()
	val functions = HashMap<String, LinkedList<FunctionDefinition>>()
	val operators = LinkedList<OperatorDefinition>()

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
		previousDeclaration = types.putIfAbsent(type.name, type)
		if(previousDeclaration == null)
			linter.messages.add(Message(
				"${type.source.getStartString()}: Declaration of type '${type.name}'.", Message.Type.DEBUG))
		else
			linter.messages.add(Message(
				"${type.source.getStartString()}: Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun declareValue(linter: Linter, value: VariableValueDeclaration) {
		var previousDeclaration = parentScope.resolveValue(value.name)
		if(previousDeclaration != null) {
			linter.messages.add(Message(
				"${value.source.getStartString()}: '${value.name}' shadows a variable.", Message.Type.WARNING))
		}
		previousDeclaration = superScope?.resolveValue(value.name)
		if(previousDeclaration != null) {
			linter.messages.add(Message(
				"${value.source.getStartString()}: Redeclaration of type '${value.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}." +
						" Use the 'override' keyword to modify it.", Message.Type.ERROR))
			return
		}
		previousDeclaration = values.putIfAbsent(value.name, value)
		if(previousDeclaration == null)
			linter.messages.add(Message(
				"${value.source.getStartString()}: Declaration of value '${value.name}'.", Message.Type.DEBUG))
		else
			linter.messages.add(Message(
			"${value.source.getStartString()}: Redeclaration of value '${value.name}'," +
					" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
	}

	override fun declareFunction(linter: Linter, function: FunctionDefinition) {
		val definitions = functions.getOrPut(function.name) { LinkedList() }
		var previousDeclaration: FunctionDefinition? = null
		functionIteration@for(declaredFunction in definitions) {
			if(declaredFunction.parameters.size != function.parameters.size)
				continue
			for(i in function.parameters.indices) {
				if(declaredFunction.parameters[i].type != function.parameters[i].type)
					continue@functionIteration
			}
			previousDeclaration = declaredFunction
			break
		}
		if(previousDeclaration == null) {
			definitions.add(function)
			linter.messages.add(Message(
				"${function.source.getStartString()}: Declaration of function '$function'.", Message.Type.DEBUG))
		} else {
			linter.messages.add(Message(
				"${function.source.getStartString()}: Redeclaration of function '$function'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
		}
	}

	override fun declareOperator(linter: Linter, operator: OperatorDefinition) {
		var previousDeclaration: OperatorDefinition? = null
		operatorIteration@for(declaredOperator in operators) {
			if(declaredOperator.name != operator.name)
				continue
			if(declaredOperator is IndexOperatorDefinition) {
				if(operator !is IndexOperatorDefinition)
					continue
				if(declaredOperator.indices.size != operator.indices.size)
					continue
				for(i in operator.indices.indices) {
					if(declaredOperator.indices[i].type != operator.indices[i].type)
						continue@operatorIteration
				}
			}
			if(declaredOperator.parameters.size != operator.parameters.size)
				continue
			for(i in operator.parameters.indices) {
				if(declaredOperator.parameters[i].type != operator.parameters[i].type)
					continue@operatorIteration
			}
			previousDeclaration = declaredOperator
			break
		}
		if(previousDeclaration == null) {
			operators.add(operator)
			linter.messages.add(Message(
				"${operator.source.getStartString()}: Declaration of operator '$operator'.", Message.Type.DEBUG))
		} else {
			linter.messages.add(Message(
				"${operator.source.getStartString()}: Redeclaration of operator '$operator'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
		}
	}

	override fun resolveValue(name: String): VariableValueDeclaration? {
		if(name == SELF_REFERENCE)
			return instanceConstant
		return values[name]
			?: superScope?.resolveValue(name)
			?: parentScope.resolveValue(name)
	}

	override fun resolveType(name: String): TypeDefinition? {
		return types[name]
			?: superScope?.resolveType(name)
			?: parentScope.resolveType(name)
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

	override fun resolveFunction(name: String, suppliedTypes: List<Type?>): FunctionDefinition? {
		functions[name]?.let { definitions ->
			functionIteration@for(function in definitions) {
				if(function.parameters.size != suppliedTypes.size)
					continue
				for(i in suppliedTypes.indices) {
					if(suppliedTypes[i]?.let { function.parameters[i].type?.accepts(it) } != true)
						continue@functionIteration
				}
				return function
			}
		}
		return superScope?.resolveFunction(name, suppliedTypes)
			?: parentScope.resolveFunction(name, suppliedTypes)
	}

	override fun resolveOperator(name: String, suppliedTypes: List<Type?>):
			OperatorDefinition? {
		operatorIteration@for(operator in operators) {
			if(operator.name != name)
				continue
			if(operator.parameters.size != suppliedTypes.size)
				continue
			for(i in suppliedTypes.indices) {
				if(suppliedTypes[i]?.let { operator.parameters[i].type?.accepts(it) } != true)
					continue@operatorIteration
			}
			return operator
		}
		return superScope?.resolveOperator(name, suppliedTypes)
			?: parentScope.resolveOperator(name, suppliedTypes)
	}

	override fun resolveIndexOperator(name: String, suppliedIndexTypes: List<Type?>, suppliedParameterTypes: List<Type?>):
			IndexOperatorDefinition? {
		operatorIteration@for(operator in operators) {
			if(operator.name != name)
				continue
			if(operator !is IndexOperatorDefinition)
				continue
			if(operator.indices.size != suppliedIndexTypes.size)
				continue
			for(i in suppliedIndexTypes.indices) {
				if(suppliedIndexTypes[i]?.let { operator.indices[i].type?.accepts(it) } != true)
					continue@operatorIteration
			}
			if(operator.parameters.size != suppliedParameterTypes.size)
				continue
			for(i in suppliedParameterTypes.indices) {
				if(suppliedParameterTypes[i]?.let { operator.parameters[i].type?.accepts(it) } != true)
					continue@operatorIteration
			}
			return operator
		}
		return superScope?.resolveIndexOperator(name, suppliedIndexTypes, suppliedParameterTypes)
			?: parentScope.resolveIndexOperator(name, suppliedIndexTypes, suppliedParameterTypes)
	}

	fun getGenericTypes(): LinkedList<Type> {
		val genericTypes = LinkedList<Type>()
		for((_, typeDefinition) in types)
			if(typeDefinition.isGeneric)
				genericTypes.add(SimpleType(typeDefinition))
		return genericTypes
	}
}