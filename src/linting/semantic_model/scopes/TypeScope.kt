package linting.semantic_model.scopes

import linting.Linter
import linting.semantic_model.definitions.*
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.Type
import linting.semantic_model.values.Function
import linting.semantic_model.values.TypeDefinition
import linting.semantic_model.values.VariableValueDeclaration
import linting.messages.Message
import java.util.*
import kotlin.collections.HashMap

class TypeScope(private val parentScope: MutableScope, private val superScope: InterfaceScope?): MutableScope() {
	var instanceConstant: VariableValueDeclaration? = null
	var typeDefinition: TypeDefinition? = null
	private val typeDefinitions = HashMap<String, TypeDefinition>()
	private val valueDeclarations = HashMap<String, VariableValueDeclaration>()
	private val initializers = LinkedList<InitializerDefinition>()
	private val operators = LinkedList<OperatorDefinition>()

	companion object {
		const val SELF_REFERENCE = "this"
	}

	fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>, superScope: InterfaceScope?): TypeScope {
		val specificTypeScope = TypeScope(parentScope, superScope)
		for((name, typeDefinition) in typeDefinitions) {
			if(typeDefinition is GenericTypeDefinition)
				continue
			specificTypeScope.typeDefinitions[name] = typeDefinition.withTypeSubstitutions(typeSubstitution)
		}
		for((name, valueDeclaration) in valueDeclarations)
			specificTypeScope.valueDeclarations[name] = valueDeclaration.withTypeSubstitutions(typeSubstitution)
		for(initializer in initializers)
			specificTypeScope.initializers.add(initializer.withTypeSubstitutions(typeSubstitution))
		for(operator in operators)
			specificTypeScope.operators.add(operator.withTypeSubstitutions(typeSubstitution))
		return specificTypeScope
	}

	fun createInstanceConstant(definition: TypeDefinition) {
		instanceConstant = VariableValueDeclaration(definition.source, SELF_REFERENCE, ObjectType(definition), null, true)
	}

	override fun subscribe(type: Type) {
		super.subscribe(type)
		for((_, typeDefinition) in typeDefinitions)
			type.onNewType(typeDefinition)
		for((_, value) in valueDeclarations)
			type.onNewValue(value)
		for(initializer in initializers)
			type.onNewInitializer(initializer)
		for(operator in operators)
			type.onNewOperator(operator)
	}

	fun ensureUniqueSignatures(linter: Linter) {
		val initializerIterator = initializers.iterator()
		val redeclarations = LinkedList<InitializerDefinition>()
		for(initializer in initializerIterator) {
			if(redeclarations.contains(initializer))
				continue
			initializerIterator.forEachRemaining { otherInitializer ->
				if(otherInitializer.parameters.size != initializer.parameters.size)
					return@forEachRemaining
				for(parameterIndex in initializer.parameters.indices) {
					if(otherInitializer.parameters[parameterIndex].type != initializer.parameters[parameterIndex].type)
						return@forEachRemaining
				}
				redeclarations.add(otherInitializer)
				linter.messages.add(
					Message(
						"${otherInitializer.source.getStartString()}: Redeclaration of" +
								" initializer '${typeDefinition?.name}(${otherInitializer.variation})'," +
								" previously declared in ${initializer.source.getStartString()}.", Message.Type.ERROR)
				)
			}
		}
		initializers.removeAll(redeclarations)
	}

	override fun declareInitializer(linter: Linter, initializer: InitializerDefinition) {
		initializers.add(initializer)
		onNewInitializer(initializer)
		linter.messages.add(Message(
			"${initializer.source.getStartString()}: " +
					"Declaration of initializer '${typeDefinition?.name}(${initializer.variation})'.",
			Message.Type.DEBUG))
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
		previousDeclaration = typeDefinitions.putIfAbsent(type.name, type)
		if(previousDeclaration == null) {
			onNewType(type)
			linter.messages.add(Message(
				"${type.source.getStartString()}: Declaration of type '${type.name}'.", Message.Type.DEBUG))
		} else {
			linter.messages.add(Message(
				"${type.source.getStartString()}: Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
		}
	}

	override fun declareFunction(linter: Linter, name: String, newImplementation: FunctionImplementation) {
		when(val existingDeclaration = valueDeclarations[name]?.value) {
			null -> {
				val newFunction = Function(newImplementation.source, newImplementation, name)
				val newValue = VariableValueDeclaration(newImplementation.source, name, newFunction.type, newFunction, true)
				valueDeclarations[name] = newValue
				onNewValue(newValue)
				linter.messages.add(Message(
					"${newImplementation.source.getStartString()}: Declaration of function '$name(${newImplementation.parameters.joinToString { p -> p.type.toString() }})'.", Message.Type.DEBUG))
			}
			is Function -> {
				existingDeclaration.addImplementation(newImplementation)
				linter.messages.add(Message(
					"${newImplementation.source.getStartString()}: Declaration of function signature '$name(${newImplementation.parameters.joinToString { p -> p.type.toString() }})'.", Message.Type.DEBUG))
			}
			else -> {
				linter.messages.add(Message(
					"${newImplementation.source.getStartString()}: Redeclaration of member '$name', " +
							"previously declared in ${existingDeclaration.source.getStartString()}.", Message.Type.ERROR))
			}
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
			onNewOperator(operator)
			linter.messages.add(Message(
				"${operator.source.getStartString()}: Declaration of operator '$operator'.", Message.Type.DEBUG))
		} else {
			linter.messages.add(Message(
				"${operator.source.getStartString()}: Redeclaration of operator '$operator'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
		}
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
		previousDeclaration = valueDeclarations.putIfAbsent(value.name, value)
		if(previousDeclaration == null) {
			onNewValue(value)
			linter.messages.add(Message(
				"${value.source.getStartString()}: Declaration of value '${value.name}'.", Message.Type.DEBUG))
		} else {
			linter.messages.add(Message(
				"${value.source.getStartString()}: Redeclaration of value '${value.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR))
		}
	}

	override fun resolveValue(name: String): VariableValueDeclaration? {
		if(name == SELF_REFERENCE)
			return instanceConstant
		return valueDeclarations[name]
			?: superScope?.resolveValue(name)
			?: parentScope.resolveValue(name)
	}

	override fun resolveType(name: String): TypeDefinition? {
		return typeDefinitions[name]
			?: superScope?.resolveType(name)
			?: parentScope.resolveType(name)
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

	override fun resolveIndexOperator(suppliedIndexTypes: List<Type?>, suppliedParameterTypes: List<Type?>):
			IndexOperatorDefinition? {
		operatorIteration@for(operator in operators) {
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
		return superScope?.resolveIndexOperator(suppliedIndexTypes, suppliedParameterTypes)
			?: parentScope.resolveIndexOperator(suppliedIndexTypes, suppliedParameterTypes)
	}
}