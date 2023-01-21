package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.*
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.*
import components.semantic_analysis.semantic_model.values.Function
import errors.internal.CompilerError
import messages.Message
import java.util.*

class TypeScope(val parentScope: MutableScope, private val superScope: InterfaceScope?): MutableScope() {
	lateinit var typeDefinition: TypeDefinition
	private val typeDefinitions = HashMap<String, TypeDefinition>()
	private val memberDeclarations = LinkedList<MemberDeclaration>()
	private val interfaceMembers = HashMap<String, InterfaceMember>()
	private val initializers = LinkedList<InitializerDefinition>()

	fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>, superScope: InterfaceScope?): TypeScope {
		val specificTypeScope = TypeScope(parentScope, superScope)
		for((name, typeDefinition) in typeDefinitions) {
			if(typeDefinition is GenericTypeDefinition)
				continue
			typeDefinition.withTypeSubstitutions(typeSubstitution) { specificDefinition ->
				specificTypeScope.typeDefinitions[name] = specificDefinition
			}
		}
		for((name, interfaceMember) in interfaceMembers)
			specificTypeScope.interfaceMembers[name] = interfaceMember.withTypeSubstitutions(typeSubstitution)
		for(initializer in initializers)
			specificTypeScope.initializers.add(initializer.withTypeSubstitutions(typeSubstitution))
		return specificTypeScope
	}

	override fun subscribe(type: Type) {
		super.subscribe(type)
		for((_, typeDefinition) in typeDefinitions)
			type.onNewType(typeDefinition)
		for((_, interfaceMember) in interfaceMembers)
			type.onNewValue(interfaceMember)
		for(initializer in initializers)
			type.onNewInitializer(initializer)
		superScope?.subscribe(type)
	}

	override fun getSurroundingDefinition(): TypeDefinition {
		return typeDefinition
	}

	fun getAbstractMembers(): List<MemberDeclaration> {
		val abstractMembers = LinkedList<MemberDeclaration>()
		if(superScope != null)
			abstractMembers.addAll(superScope.getAbstractMembers())
		for(memberDeclaration in memberDeclarations) {
			if(memberDeclaration.isAbstract)
				abstractMembers.add(memberDeclaration)
		}
		return abstractMembers
	}

	fun inheritSignatures() {
		for((_, memberDeclaration) in interfaceMembers) {
			memberDeclaration.superMember = superScope?.resolveValue(memberDeclaration.name)
			val function = memberDeclaration.value as? Function ?: continue
			function.functionType.superFunctionType = memberDeclaration.superMember?.type as? FunctionType
		}
	}

	fun ensureTrivialInitializers(linter: Linter) {
		for(initializer in initializers) {
			if(initializer.genericParameters.isNotEmpty())
				linter.addMessage(initializer.source, "Object initializers can not take type parameters.", Message.Type.ERROR)
			if(initializer.parameters.isNotEmpty())
				linter.addMessage(initializer.source, "Object initializers can not take parameters.", Message.Type.ERROR)
		}
	}

	fun ensureUniqueInitializerSignatures(linter: Linter) {
		val redeclarations = LinkedList<InitializerDefinition>()
		for(initializerIndex in 0 until initializers.size - 1) {
			val initializer = initializers[initializerIndex]
			if(redeclarations.contains(initializer))
				continue
			initializerIteration@for(otherInitializerIndex in initializerIndex + 1 until initializers.size) {
				val otherInitializer = initializers[otherInitializerIndex]
				if(otherInitializer.parameters.size != initializer.parameters.size)
					continue
				for(parameterIndex in initializer.parameters.indices) {
					if(otherInitializer.parameters[parameterIndex].type != initializer.parameters[parameterIndex].type)
						continue@initializerIteration
				}
				redeclarations.add(otherInitializer)
				linter.addMessage(otherInitializer.source, "Redeclaration of initializer '$otherInitializer'," +
						" previously declared in ${initializer.source.getStartString()}.", Message.Type.ERROR)
			}
		}
		initializers.removeAll(redeclarations)
	}

	fun ensureNoAbstractMembers(linter: Linter) {
		for(memberDeclaration in memberDeclarations) {
			if(memberDeclaration.isAbstract) {
				linter.addMessage(memberDeclaration.source,
					"Abstract member '${memberDeclaration.memberIdentifier}' is not allowed" +
						" in non-abstract class '${typeDefinition.name}'.", Message.Type.ERROR)
			}
		}
	}

	fun ensureAbstractSuperMembersImplemented(linter: Linter) {
		val missingOverrides = LinkedHashMap<TypeDefinition, LinkedList<MemberDeclaration>>()
		val abstractSuperMembers = superScope?.getAbstractMembers() ?: return
		for(abstractSuperMember in abstractSuperMembers) {
			//val overridingMember = memberDeclarations[abstractSuperMember.memberIdentifier]
			val overridingMember = memberDeclarations.find { memberDeclaration ->
				memberDeclaration.memberIdentifier == abstractSuperMember.memberIdentifier }
			if(!abstractSuperMember.canBeOverriddenBy(overridingMember)) {
				val parentDefinition = abstractSuperMember.parentDefinition
					?: throw CompilerError("Member is missing parent definition.")
				val missingOverridesFromType = missingOverrides.getOrPut(parentDefinition) { LinkedList() }
				missingOverridesFromType.add(abstractSuperMember)
			}
		}
		if(missingOverrides.isEmpty())
			return
		var message = "Non-abstract class '${typeDefinition.name}' does not implement the following inherited members:"
		for((parent, missingMembers) in missingOverrides) {
			message += "\n- ${parent.name}"
			for(member in missingMembers)
				message += "\n  - ${member.memberIdentifier}"
		}
		linter.addMessage(typeDefinition.source, message, Message.Type.ERROR)
	}

	override fun declareInitializer(linter: Linter, initializer: InitializerDefinition) {
		initializers.add(initializer)
		onNewInitializer(initializer)
		linter.addMessage(initializer.source,
			"Declaration of initializer '$initializer'.", Message.Type.DEBUG)
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
		if(value !is InterfaceMember)
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
		memberDeclarations.add(value)
		onNewValue(value)
		linter.addMessage(value.source, "Declaration of member '${typeDefinition.name}.${value.name}'.", Message.Type.DEBUG)
	}

	override fun declareFunction(linter: Linter, name: String, newImplementation: FunctionImplementation) {
		when(val existingInterfaceMember = interfaceMembers[name]?.value) {
			null -> {
				val newFunction = Function(newImplementation.source, name)
				newFunction.addImplementation(newImplementation)
				typeDefinition.addUnits(newFunction)
				val newValue = PropertyDeclaration(newImplementation.source, name, newFunction.type, newFunction,
					newFunction.isAbstract)
				newValue.parentDefinition = typeDefinition
				interfaceMembers[name] = newValue
				onNewValue(newValue)
			}
			is Function -> {
				existingInterfaceMember.addImplementation(newImplementation)
			}
			else -> {
				linter.addMessage(newImplementation.source, "Redeclaration of member '${typeDefinition.name}.$name', " +
							"previously declared in ${existingInterfaceMember.source.getStartString()}.", Message.Type.ERROR)
				return
			}
		}
		memberDeclarations.add(newImplementation)
		linter.addMessage(newImplementation.source, "Declaration of function " +
				"'${typeDefinition.name}.$name${newImplementation.signature.toString(false)}'.", Message.Type.DEBUG)
	}

	override fun declareOperator(linter: Linter, kind: Operator.Kind, newImplementation: FunctionImplementation) {
		val name = kind.stringRepresentation
		when(val existingInterfaceMember = interfaceMembers[name]?.value) {
			null -> {
				val newOperator = Operator(newImplementation.source, kind)
				newOperator.addImplementation(newImplementation)
				typeDefinition.addUnits(newOperator)
				val newValue = PropertyDeclaration(newImplementation.source, name, newOperator.type, newOperator, newOperator.isAbstract)
				newValue.parentDefinition = typeDefinition
				interfaceMembers[name] = newValue
				onNewValue(newValue)
			}
			is Function -> {
				existingInterfaceMember.addImplementation(newImplementation)
			}
			else -> {
				linter.addMessage(newImplementation.source, "Redeclaration of member '${typeDefinition.name}.$name', " +
					"previously declared in ${existingInterfaceMember.source.getStartString()}.", Message.Type.ERROR)
				return
			}
		}
		memberDeclarations.add(newImplementation)
		linter.addMessage(newImplementation.source, "Declaration of operator " +
			"'${typeDefinition.name}.$name${newImplementation.signature.toString(false)}'.", Message.Type.DEBUG)
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
