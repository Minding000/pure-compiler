package linting.semantic_model.scopes

import linting.Linter
import linting.semantic_model.definitions.*
import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.Type
import linting.semantic_model.values.Function
import linting.semantic_model.values.Instance
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValueDeclaration
import messages.Message
import java.util.*

class TypeScope(private val parentScope: MutableScope, private val superScope: InterfaceScope?): MutableScope() {
	lateinit var typeDefinition: TypeDefinition
	private val typeDefinitions = HashMap<String, TypeDefinition>()
	private val valueDeclarations = HashMap<String, VariableValueDeclaration>()
	private val initializers = LinkedList<InitializerDefinition>()
	private val operators = LinkedList<OperatorDefinition>()

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

	override fun subscribe(type: Type) {
		super.subscribe(type)
		superScope?.subscribe(type)
		for((_, typeDefinition) in typeDefinitions)
			type.onNewType(typeDefinition)
		for((_, valueDeclaration) in valueDeclarations)
			type.onNewValue(valueDeclaration)
		for(initializer in initializers)
			type.onNewInitializer(initializer)
		for(operator in operators)
			type.onNewOperator(operator)
	}

	fun inheritSignatures() {
		for((_, valueDeclaration) in valueDeclarations) {
			if(valueDeclaration.value !is Function)
				continue
			val superValue = superScope?.resolveValue(valueDeclaration.name)
			valueDeclaration.value.superFunction = superValue?.value as? Function
		}
	}

	fun ensureUniqueInitializerSignatures(linter: Linter) {
		val redeclarations = LinkedList<InitializerDefinition>()
		for(initializerIndex in 0 until initializers.size - 1) {
			val initializer = initializers[initializerIndex]
			if(redeclarations.contains(initializer))
				continue
			for(otherInitializerIndex in initializerIndex + 1 until  initializers.size) {
				val otherInitializer = initializers[otherInitializerIndex]
				if(otherInitializer.parameters.size != initializer.parameters.size)
					continue
				for(parameterIndex in initializer.parameters.indices) {
					if(otherInitializer.parameters[parameterIndex].type != initializer.parameters[parameterIndex].type)
						continue
				}
				redeclarations.add(otherInitializer)
				linter.addMessage(otherInitializer.source, "Redeclaration of" +
						" initializer '${typeDefinition.name}(${otherInitializer.variation})'," +
						" previously declared in ${initializer.source.getStartString()}.", Message.Type.ERROR)
			}
		}
		initializers.removeAll(redeclarations)
	}

	override fun declareInitializer(linter: Linter, initializer: InitializerDefinition) {
		initializers.add(initializer)
		onNewInitializer(initializer)
		linter.addMessage(initializer.source, "Declaration of initializer '${typeDefinition.name}(${initializer.variation})'.",
			Message.Type.DEBUG)
	}

	override fun declareType(linter: Linter, type: TypeDefinition) {
		var previousDeclaration = parentScope.resolveType(type.name)
		if(previousDeclaration != null) {
			linter.addMessage(type.source, "'${type.name}' shadows a type.", Message.Type.WARNING)
		}
		previousDeclaration = superScope?.resolveType(type.name)
		if(previousDeclaration != null) {
			linter.addMessage(type.source, "Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
			return
		}
		previousDeclaration = typeDefinitions.putIfAbsent(type.name, type)
		if(previousDeclaration == null) {
			onNewType(type)
			linter.addMessage(type.source, "Declaration of type '${type.name}'.", Message.Type.DEBUG)
		} else {
			linter.addMessage(type.source, "Redeclaration of type '${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
		}
	}

	override fun declareValue(linter: Linter, value: VariableValueDeclaration) {
		var previousDeclaration = parentScope.resolveValue(value.name)
		if(previousDeclaration != null) {
			linter.addMessage(value.source, "'${value.name}' shadows a variable.", Message.Type.WARNING)
		}
		previousDeclaration = superScope?.resolveValue(value.name)
		if(previousDeclaration != null) {
			linter.addMessage(value.source, "Redeclaration of value '${value.name}'," +
				" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
			return
		}
		previousDeclaration = valueDeclarations.putIfAbsent(value.name, value)
		if(previousDeclaration == null) {
			if(value is Instance)
				value.setType(typeDefinition)
			onNewValue(value)
			linter.addMessage(value.source, "Declaration of value '${value.name}'.", Message.Type.DEBUG)
		} else {
			linter.addMessage(value.source, "Redeclaration of value '${value.name}'," +
				" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
		}
	}

	override fun declareFunction(linter: Linter, name: String, newImplementation: FunctionImplementation) {
		when(val existingDeclaration = valueDeclarations[name]?.value) {
			null -> {
				val newFunction = Function(newImplementation.source, newImplementation, name)
				typeDefinition.units.add(newFunction)
				val newValue = VariableValueDeclaration(newImplementation.source, name, newFunction.type, newFunction)
				valueDeclarations[name] = newValue
				onNewValue(newValue)
			}
			is Function -> {
				existingDeclaration.addImplementation(newImplementation)
			}
			else -> {
				linter.addMessage(newImplementation.source, "Redeclaration of member '$name', " +
							"previously declared in ${existingDeclaration.source.getStartString()}.", Message.Type.ERROR)
				return
			}
		}
		linter.addMessage(newImplementation.source, "Declaration of function signature " +
				"'$name${newImplementation.signature.toString(false)}'.",
			Message.Type.DEBUG)
	}

	override fun declareOperator(linter: Linter, operator: OperatorDefinition) {
		var previousDeclaration: OperatorDefinition? = null
		operatorIteration@for(declaredOperator in operators) {
			if(declaredOperator.name != operator.name)
				continue
			if(declaredOperator is IndexOperatorDefinition) {
				if(operator !is IndexOperatorDefinition)
					continue
				if(declaredOperator.indexParameters.size != operator.indexParameters.size)
					continue
				for(i in operator.indexParameters.indices) {
					if(declaredOperator.indexParameters[i].type != operator.indexParameters[i].type)
						continue@operatorIteration
				}
			}
			if(declaredOperator.valueParameters.size != operator.valueParameters.size)
				continue
			for(i in operator.valueParameters.indices) {
				if(declaredOperator.valueParameters[i].type != operator.valueParameters[i].type)
					continue@operatorIteration
			}
			previousDeclaration = declaredOperator
			break
		}
		if(previousDeclaration == null) {
			operators.add(operator)
			onNewOperator(operator)
			linter.addMessage(operator.source, "Declaration of operator '$operator'.", Message.Type.DEBUG)
		} else {
			linter.addMessage(operator.source, "Redeclaration of operator '$operator'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
		}
	}

	override fun resolveValue(name: String): VariableValueDeclaration? {
		return valueDeclarations[name]
			?: superScope?.resolveValue(name)
			?: parentScope.resolveValue(name)
	}

	override fun resolveType(name: String): TypeDefinition? {
		return typeDefinitions[name]
			?: superScope?.resolveType(name)
			?: parentScope.resolveType(name)
	}

	//TODO this function is outdated (see InterfaceScope)
	override fun resolveOperator(name: String, suppliedValues: List<Value>):
			OperatorDefinition? {
		operatorIteration@for(operator in operators) {
			if(operator.name != name)
				continue
			if(operator.valueParameters.size != suppliedValues.size)
				continue
			for(i in suppliedValues.indices) {
				if(!suppliedValues[i].isAssignableTo(operator.valueParameters[i].type))
					continue@operatorIteration
			}
			return operator
		}
		return superScope?.resolveOperator(name, suppliedValues)
			?: parentScope.resolveOperator(name, suppliedValues)
	}

	//TODO this function is outdated (see InterfaceScope)
	override fun resolveIndexOperator(suppliedTypes: List<Type>, suppliedIndexValues: List<Value>,
									  suppliedParameterValues: List<Value>): IndexOperatorDefinition? {
		operatorIteration@for(operator in operators) {
			if(operator !is IndexOperatorDefinition)
				continue
			if(operator.indexParameters.size != suppliedIndexValues.size)
				continue
			if(operator.valueParameters.size != suppliedParameterValues.size)
				continue
			for(i in suppliedIndexValues.indices) {
				if(!suppliedIndexValues[i].isAssignableTo(operator.indexParameters[i].type))
					continue@operatorIteration
			}
			for(i in suppliedParameterValues.indices) {
				if(!suppliedParameterValues[i].isAssignableTo(operator.valueParameters[i].type))
					continue@operatorIteration
			}
			return operator
		}
		return superScope?.resolveIndexOperator(suppliedTypes, suppliedIndexValues, suppliedParameterValues)
			?: parentScope.resolveIndexOperator(suppliedTypes, suppliedIndexValues, suppliedParameterValues)
	}

	fun getGenericTypes(): LinkedList<ObjectType> {
		val genericTypes = LinkedList<ObjectType>()
		for((_, typeDefinition) in typeDefinitions)
			if(typeDefinition is GenericTypeDefinition)
				genericTypes.add(ObjectType(typeDefinition))
		return genericTypes
	}

	fun getGenericTypeDefinitions(): LinkedList<GenericTypeDefinition> {
		val genericTypes = LinkedList<GenericTypeDefinition>()
		for((_, typeDefinition) in typeDefinitions)
			if(typeDefinition is GenericTypeDefinition)
				genericTypes.add(typeDefinition)
		return genericTypes
	}
}
