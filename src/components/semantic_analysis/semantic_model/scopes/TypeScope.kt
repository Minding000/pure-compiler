package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.*
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.*
import components.semantic_analysis.semantic_model.values.Function
import errors.internal.CompilerError
import messages.Message
import java.util.*
import kotlin.collections.LinkedHashMap

class TypeScope(private val parentScope: MutableScope, private val superScope: InterfaceScope?): MutableScope() {
	lateinit var typeDefinition: TypeDefinition
	private val typeDefinitions = HashMap<String, TypeDefinition>()
	private val memberDeclarations = HashMap<String, MemberDeclaration>()
	private val interfaceMembers = HashMap<String, MemberDeclaration>()
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
		for((name, memberDeclaration) in interfaceMembers)
			specificTypeScope.interfaceMembers[name] = memberDeclaration.withTypeSubstitutions(typeSubstitution)
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
		for((_, memberDeclaration) in interfaceMembers)
			type.onNewValue(memberDeclaration)
		for(initializer in initializers)
			type.onNewInitializer(initializer)
		for(operator in operators)
			type.onNewOperator(operator)
	}

	override fun getSurroundingDefinition(): TypeDefinition {
		return typeDefinition
	}

	fun getAbstractMembers(): List<MemberDeclaration> {
		val abstractMembers = LinkedList<MemberDeclaration>()
		if(superScope != null)
			abstractMembers.addAll(superScope.getAbstractMembers())
		for((_, memberDeclaration) in memberDeclarations) {
			if(memberDeclaration.isAbstract)
				abstractMembers.add(memberDeclaration)
		}
		return abstractMembers
	}

	fun inheritSignatures() {
		for((_, memberDeclaration) in interfaceMembers) {
			val function = memberDeclaration.value as? Function ?: continue
			val superValue = superScope?.resolveValue(memberDeclaration.name)
			function.superFunction = superValue?.value as? Function
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
		for((_, memberDeclaration) in memberDeclarations) {
			if(memberDeclaration.isAbstract) {
				linter.addMessage(memberDeclaration.source,
					"Abstract member '${memberDeclaration.signatureString}' is not allowed in non-abstract class '${typeDefinition.name}'.",
					Message.Type.ERROR)
			}
		}
	}

	fun ensureAbstractSuperMembersImplemented(linter: Linter) {
		//TODO differentiate between declarations and members: functions are members and signatures are declarations
		// - make extra fields in TypeScope: properties and signatures?
		// - Make FunctionImplementations MemberDeclarations as well
		// - How would a string representation of members represent functions properly?
		//   - (String) =>| & (String, String) =>| should be able to override (String) =>| and (String, String) =>|
		val missingOverrides = LinkedHashMap<TypeDefinition, LinkedList<MemberDeclaration>>()
		val abstractSuperMembers = superScope?.getAbstractMembers() ?: return
		for(abstractSuperMember in abstractSuperMembers) {
			val overridingMember = memberDeclarations[abstractSuperMember.signatureString]
			if(!abstractSuperMember.canBeOverriddenBy(overridingMember)) {
				val missingOverridesFromType = missingOverrides.getOrPut(abstractSuperMember.parentDefinition) {
					LinkedList() }
				missingOverridesFromType.add(abstractSuperMember)
			}
		}
		if(missingOverrides.isEmpty())
			return
		var message = "Non-abstract class '${typeDefinition.name}' does not implement the following inherited members:"
		for((parent, missingMembers) in missingOverrides) {
			message += "\n- ${parent.name}"
			for(member in missingMembers)
				message += "\n  - ${member.signatureString}"
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

	override fun declareValue(linter: Linter, value: ValueDeclaration) {
		if(value !is MemberDeclaration)
			throw CompilerError("Tried to declare non-member of type '${value.javaClass.simpleName}' in type scope.")
		value.parentDefinition = typeDefinition
		var previousDeclaration = parentScope.resolveValue(value.name)
		if(previousDeclaration != null)
			linter.addMessage(value.source, "'${value.name}' shadows a member," +
				" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.WARNING)
		previousDeclaration = superScope?.resolveValue(value.name) ?: interfaceMembers.putIfAbsent(value.name, value)
		if(previousDeclaration != null) {
			linter.addMessage(value.source, "Redeclaration of member '${typeDefinition.name}.${value.name}'," +
				" previously declared in ${previousDeclaration.source.getStartString()}.", Message.Type.ERROR)
			return
		}
		if(value is Instance)
			value.setType(typeDefinition)
		memberDeclarations[value.signatureString] = value
		onNewValue(value)
		linter.addMessage(value.source, "Declaration of member '${typeDefinition.name}.${value.name}'.", Message.Type.DEBUG)
	}

	override fun declareFunction(linter: Linter, name: String, newImplementation: FunctionImplementation) {
		when(val existingDeclaration = interfaceMembers[name]?.value) {
			null -> {
				val newFunction = Function(newImplementation.source, newImplementation, name)
				typeDefinition.addUnits(newFunction)
				val newValue = PropertyDeclaration(newImplementation.source, name, newFunction.type, newFunction,
					newFunction.isAbstract)
				newValue.parentDefinition = typeDefinition
				interfaceMembers[name] = newValue
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
		newImplementation.parentDefinition = typeDefinition
		memberDeclarations[newImplementation.signatureString] = newImplementation
		linter.addMessage(newImplementation.source, "Declaration of function " +
				"'${typeDefinition.name}.$name${newImplementation.signature.toString(false)}'.",
			Message.Type.DEBUG)
	}

	override fun declareOperator(linter: Linter, operator: OperatorDefinition) {
		operators.add(operator)
		onNewOperator(operator)
		linter.addMessage(operator.source, "Declaration of operator '${typeDefinition.name}$operator'.", Message.Type.DEBUG)
	}

	override fun resolveValue(name: String): ValueDeclaration? {
		return interfaceMembers[name]
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
