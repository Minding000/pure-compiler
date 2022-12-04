package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.*
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.Instance
import components.semantic_analysis.semantic_model.values.VariableValueDeclaration
import messages.Message
import java.util.*
import kotlin.collections.HashMap

class TypeScope(private val parentScope: MutableScope, private val superScope: InterfaceScope?): MutableScope() {
	lateinit var typeDefinition: TypeDefinition
	private val typeDefinitions = HashMap<String, TypeDefinition>()
	private val valueDeclarations = HashMap<String, VariableValueDeclaration>()
	private val initializers = LinkedList<InitializerDefinition>()
	private val operators = LinkedList<OperatorDefinition>()

	fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>, superScope: InterfaceScope?): TypeScope {
		val specificTypeScope = TypeScope(parentScope, superScope)
		for((name, typeDefinition) in typeDefinitions) {
			if(typeDefinition is GenericTypeDefinition)
				continue
			typeDefinition.withTypeSubstitutions(typeSubstitution) { specificDefinition ->
				specificTypeScope.typeDefinitions[name] = specificDefinition
			}
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

	override fun getSurroundingDefinition(): TypeDefinition {
		return typeDefinition
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
			initializerIteration@for(otherInitializerIndex in initializerIndex + 1 until  initializers.size) {
				val otherInitializer = initializers[otherInitializerIndex]
				if(otherInitializer.parameters.size != initializer.parameters.size)
					continue
				for(parameterIndex in initializer.parameters.indices) {
					if(otherInitializer.parameters[parameterIndex].type != initializer.parameters[parameterIndex].type)
						continue@initializerIteration
				}
				redeclarations.add(otherInitializer)
				linter.addMessage(otherInitializer.source, "Redeclaration of" +
						" initializer '${otherInitializer.toString(typeDefinition)}'," +
						" previously declared in ${initializer.source.getStartString()}.", Message.Type.ERROR)
			}
		}
		initializers.removeAll(redeclarations)
	}

	fun ensureUniqueOperatorSignatures(linter: Linter) {
		val redeclarations = LinkedList<OperatorDefinition>()
		for(operatorIndex in 0 until operators.size - 1) {
			val operator = operators[operatorIndex]
			if(redeclarations.contains(operator))
				continue
			operatorIteration@for(otherOperatorIndex in operatorIndex + 1 until  operators.size) {
				val otherOperator = operators[otherOperatorIndex]
				if(otherOperator.name != operator.name)
					continue
				if(otherOperator is IndexOperatorDefinition && operator is IndexOperatorDefinition) {
					if(otherOperator.indexParameters.size != operator.indexParameters.size)
						continue
					for(indexParameterIndex in operator.indexParameters.indices) {
						if(otherOperator.indexParameters[indexParameterIndex].type != operator.indexParameters[indexParameterIndex].type)
							continue@operatorIteration
					}
				}
				if(otherOperator.valueParameters.size != operator.valueParameters.size)
					continue
				for(valueParameterIndex in operator.valueParameters.indices) {
					if(otherOperator.valueParameters[valueParameterIndex].type != operator.valueParameters[valueParameterIndex].type)
						continue@operatorIteration
				}
				redeclarations.add(otherOperator)
				linter.addMessage(otherOperator.source, "Redeclaration of operator '${typeDefinition.name}$otherOperator'," +
						" previously declared in ${operator.source.getStartString()}.", Message.Type.ERROR)
			}
		}
		operators.removeAll(redeclarations) //TODO redeclarations are not removed from subscribed types (same for initializers)
	}

	fun ensureNoAbstractMembers(linter: Linter) {
		for((name, valueDeclaration) in valueDeclarations) {
			if(valueDeclaration.isAbstract) {
				linter.addMessage(valueDeclaration.source,
					"Abstract member '$name' is not allowed in non-abstract class '${typeDefinition.name}'.",
					Message.Type.ERROR)
			}
		}
	}

	fun ensureAbstractSuperMembersImplemented(linter: Linter) {
		val missingOverrides = HashMap<TypeDefinition, LinkedList<VariableValueDeclaration>>()
		val abstractSuperMembers = superScope?.getAbstractMembers() ?: return
		for(abstractSuperMember in abstractSuperMembers) {
			if(valueDeclarations[abstractSuperMember.definition.name] == null) {
				//TODO continue abstract modifier implementation here
				//val missingOverridesFromType = missingOverrides.getOrPut(abstractSuperMember.definition) { LinkedList() }
				//missingOverridesFromType.add(abstractSuperMember.definition)
			}
		}
		if(missingOverrides.isEmpty())
			return
		var message = "Non-abstract class '${typeDefinition.name}' does not implement to following inherited members:\n"
		for((name, definition) in missingOverrides) {
			message += "  - $name\n"
			for(value in definition)
				message += "    - ${value.name}\n"
		}
		linter.addMessage(typeDefinition.source, message, Message.Type.ERROR)
	}

	override fun declareInitializer(linter: Linter, initializer: InitializerDefinition) {
		initializers.add(initializer)
		onNewInitializer(initializer)
		linter.addMessage(initializer.source,
			"Declaration of initializer '${initializer.toString(typeDefinition)}'.", Message.Type.DEBUG)
	}

	override fun declareType(linter: Linter, type: TypeDefinition) {
		var previousDeclaration = parentScope.resolveType(type.name)
		if(previousDeclaration != null)
			linter.addMessage(type.source, "'${type.name}' shadows a type," +
				" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.WARNING)
		previousDeclaration = superScope?.resolveType(type.name) ?: typeDefinitions.putIfAbsent(type.name, type)
		if(previousDeclaration != null) {
			linter.addMessage(type.source, "Redeclaration of type '${typeDefinition.name}.${type.name}'," +
						" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
			return
		}
		onNewType(type)
		linter.addMessage(type.source, "Declaration of type '${typeDefinition.name}.${type.name}'.", Message.Type.DEBUG)
	}

	override fun declareValue(linter: Linter, value: VariableValueDeclaration) {
		var previousDeclaration = parentScope.resolveValue(value.name)
		if(previousDeclaration != null)
			linter.addMessage(value.source, "'${value.name}' shadows a member," +
				" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.WARNING)
		previousDeclaration = superScope?.resolveValue(value.name) ?: valueDeclarations.putIfAbsent(value.name, value)
		if(previousDeclaration != null) {
			linter.addMessage(value.source, "Redeclaration of member '${typeDefinition.name}.${value.name}'," +
				" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
			return
		}
		if(value is Instance)
			value.setType(typeDefinition)
		onNewValue(value)
		linter.addMessage(value.source, "Declaration of member '${typeDefinition.name}.${value.name}'.", Message.Type.DEBUG)
	}

	override fun declareFunction(linter: Linter, name: String, newImplementation: FunctionImplementation) {
		when(val existingDeclaration = valueDeclarations[name]?.value) {
			null -> {
				val newFunction = Function(newImplementation.source, newImplementation, name)
				typeDefinition.addUnits(newFunction)
				val newValue = VariableValueDeclaration(newImplementation.source, name, newFunction.type, newFunction,
					newFunction.isAbstract)
				valueDeclarations[name] = newValue
				onNewValue(newValue)
			}
			is Function -> {
				existingDeclaration.addImplementation(newImplementation)
			}
			else -> {
				linter.addMessage(newImplementation.source, "Redeclaration of member '${typeDefinition.name}.$name', " +
							"previously declared in ${existingDeclaration.source.getStartString()}.",
					Message.Type.ERROR)
				return
			}
		}
		linter.addMessage(newImplementation.source, "Declaration of function " +
				"'${typeDefinition.name}.$name${newImplementation.signature.toString(false)}'.",
			Message.Type.DEBUG)
	}

	override fun declareOperator(linter: Linter, operator: OperatorDefinition) {
		operators.add(operator)
		onNewOperator(operator)
		linter.addMessage(operator.source, "Declaration of operator '${typeDefinition.name}$operator'.", Message.Type.DEBUG)
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

	fun getGenericTypeDefinitions(): LinkedList<GenericTypeDefinition> {
		val genericTypes = LinkedList<GenericTypeDefinition>()
		for((_, typeDefinition) in typeDefinitions)
			if(typeDefinition is GenericTypeDefinition)
				genericTypes.add(typeDefinition)
		return genericTypes
	}
}
