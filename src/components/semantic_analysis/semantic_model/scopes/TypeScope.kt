package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.*
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.*
import components.semantic_analysis.semantic_model.values.Function
import errors.internal.CompilerError
import logger.issues.definition.*
import java.util.*

class TypeScope(val enclosingScope: MutableScope, private val superScope: InterfaceScope?): MutableScope() {
	lateinit var typeDefinition: TypeDefinition
	private val typeDefinitions = LinkedHashMap<String, TypeDefinition>()
	val memberDeclarations = LinkedList<MemberDeclaration>()
	private val interfaceMembers = HashMap<String, InterfaceMember>()
	val initializers = LinkedList<InitializerDefinition>()

	fun withTypeSubstitutions(linter: Linter, typeSubstitution: Map<TypeDefinition, Type>, superScope: InterfaceScope?): TypeScope {
		val specificTypeScope = TypeScope(enclosingScope, superScope)
		for((name, typeDefinition) in typeDefinitions) {
			if(typeDefinition is GenericTypeDefinition)
				continue
			if(typeDefinition.isBound) {
				typeDefinition.withTypeSubstitutions(linter, typeSubstitution) { specificDefinition ->
					specificTypeScope.typeDefinitions[name] = specificDefinition
				}
			} else {
				specificTypeScope.typeDefinitions[name] = typeDefinition
			}
		}
		for((name, interfaceMember) in interfaceMembers) {
			specificTypeScope.interfaceMembers[name] = if(interfaceMember.isStatic)
				interfaceMember
			else
				interfaceMember.withTypeSubstitutions(linter, typeSubstitution)
		}
		for(initializer in initializers)
			specificTypeScope.initializers.add(initializer.withTypeSubstitutions(linter, typeSubstitution))
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
		if(type !is StaticType)
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

	fun getPropertiesToBeInitialized(): List<PropertyDeclaration> {
		val propertiesToBeInitialized = LinkedList<PropertyDeclaration>()
		if(superScope != null)
			propertiesToBeInitialized.addAll(superScope.getPropertiesToBeInitialized())
		propertiesToBeInitialized.addAll(memberDeclarations.filterIsInstance<PropertyDeclaration>().filter { member ->
			!member.isStatic && member.value == null })
		return propertiesToBeInitialized
	}

	fun inheritSignatures() {
		for(initializer in initializers)
			initializer.superInitializer = superScope?.getSuperInitializer(initializer)
		for((_, interfaceMember) in interfaceMembers) {
			interfaceMember.superMember = superScope?.resolveValue(interfaceMember.name)
			val function = interfaceMember.value as? Function ?: continue
			function.functionType.superFunctionType = interfaceMember.superMember?.type as? FunctionType
		}
	}

	fun ensureTrivialInitializers(linter: Linter) {
		for(initializer in initializers) {
			if(initializer.typeParameters.isNotEmpty())
				linter.addIssue(ObjectInitializerTakingTypeParameters(initializer.source))
			if(initializer.parameters.isNotEmpty())
				linter.addIssue(ObjectInitializerTakingParameters(initializer.source))
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
				linter.addIssue(Redeclaration(otherInitializer.source, "initializer", otherInitializer.toString(), initializer.source))
			}
		}
		initializers.removeAll(redeclarations)
	}

	fun ensureNoAbstractMembers(linter: Linter) {
		for(memberDeclaration in memberDeclarations) {
			if(memberDeclaration.isAbstract)
				linter.addIssue(AbstractMemberInNonAbstractTypeDefinition(memberDeclaration, typeDefinition))
		}
	}

	fun ensureAbstractSuperMembersImplemented(linter: Linter) {
		val missingOverrides = LinkedHashMap<TypeDefinition, LinkedList<MemberDeclaration>>()
		val abstractSuperMembers = superScope?.getAbstractMembers() ?: return
		for(abstractSuperMember in abstractSuperMembers) {
			val overridingMember = memberDeclarations.find { memberDeclaration ->
				memberDeclaration.memberIdentifier == abstractSuperMember.memberIdentifier }
			if(!abstractSuperMember.canBeOverriddenBy(overridingMember)) {
				val parentDefinition = abstractSuperMember.parentDefinition
					?: throw CompilerError(typeDefinition.source, "Member is missing parent definition.")
				val missingOverridesFromType = missingOverrides.getOrPut(parentDefinition) { LinkedList() }
				missingOverridesFromType.add(abstractSuperMember)
			}
		}
		if(missingOverrides.isEmpty())
			return
		linter.addIssue(MissingImplementations(typeDefinition, missingOverrides))
	}

	override fun declareInitializer(linter: Linter, initializer: InitializerDefinition) {
		initializers.add(initializer)
		onNewInitializer(initializer)
		memberDeclarations.add(initializer)
		linter.addIssue(Declaration(initializer.source, "initializer", initializer.toString()))
	}

	override fun declareType(linter: Linter, type: TypeDefinition) {
		var previousDeclaration = enclosingScope.resolveType(type.name)
		if(previousDeclaration != null)
			linter.addIssue(ShadowsElement(type.source, "type", type.name, previousDeclaration.source))
		previousDeclaration = superScope?.resolveType(type.name) ?: typeDefinitions.putIfAbsent(type.name, type)
		if(previousDeclaration != null) {
			linter.addIssue(Redeclaration(type.source, "type", "${typeDefinition.name}.${type.name}",
				previousDeclaration.source))
			return
		}
		onNewType(type)
		linter.addIssue(Declaration(type.source, "type", "${typeDefinition.name}.${type.name}"))
	}

	override fun declareValue(linter: Linter, value: ValueDeclaration) {
		if(value !is InterfaceMember)
			throw CompilerError(typeDefinition.source,
				"Tried to declare non-member of type '${value.javaClass.simpleName}' in type scope.")
		value.parentDefinition = typeDefinition
		var previousDeclaration = enclosingScope.resolveValue(value.name)
		if(previousDeclaration != null)
			linter.addIssue(ShadowsElement(value.source, "member", value.name, previousDeclaration.source))
		previousDeclaration = superScope?.resolveValue(value.name) ?: interfaceMembers.putIfAbsent(value.name, value)
		if(previousDeclaration != null) {
			linter.addIssue(Redeclaration(value.source, "member", "${typeDefinition.name}.${value.name}",
				previousDeclaration.source))
			return
		}
		if(value is Instance)
			value.setType(typeDefinition)
		memberDeclarations.add(value)
		onNewValue(value)
		linter.addIssue(Declaration(value.source, "member", "${typeDefinition.name}.${value.name}"))
	}

	override fun declareFunction(linter: Linter, name: String, newImplementation: FunctionImplementation) {
		when(val existingInterfaceMember = interfaceMembers[name]?.value) {
			null -> {
				val newFunction = Function(newImplementation.source, this, name)
				newFunction.addImplementation(newImplementation)
				//TODO Why add the function instead of the property?
				typeDefinition.addUnits(newFunction)
				//TODO add two properties (static & instance) - same for operators
				// The function should have both of these types
				// - only add instance values to ObjectType
				// - access InstanceAccess through StaticType
				val newValue = PropertyDeclaration(newImplementation.source, this, name, newFunction.type, newFunction, false,
					newFunction.isAbstract)
				newValue.parentDefinition = typeDefinition
				interfaceMembers[name] = newValue
				onNewValue(newValue)
			}
			is Function -> {
				existingInterfaceMember.addImplementation(newImplementation)
			}
			else -> {
				linter.addIssue(Redeclaration(newImplementation.source, "member", "${typeDefinition.name}.$name",
					existingInterfaceMember.source))
				return
			}
		}
		memberDeclarations.add(newImplementation)
		val signature = "${typeDefinition.name}.$name${newImplementation.signature.toString(false)}"
		linter.addIssue(Declaration(newImplementation.source, "function", signature))
	}

	override fun declareOperator(linter: Linter, kind: Operator.Kind, newImplementation: FunctionImplementation) {
		val name = kind.stringRepresentation
		when(val existingInterfaceMember = interfaceMembers[name]?.value) {
			null -> {
				val newOperator = Operator(newImplementation.source, this, kind)
				newOperator.addImplementation(newImplementation)
				typeDefinition.addUnits(newOperator) //TODO Why add the operator instead of the property?
				val newValue = PropertyDeclaration(newImplementation.source, this, name, newOperator.type, newOperator, false,
					newOperator.isAbstract)
				newValue.parentDefinition = typeDefinition
				interfaceMembers[name] = newValue
				onNewValue(newValue)
			}
			is Function -> {
				existingInterfaceMember.addImplementation(newImplementation)
			}
			else -> {
				linter.addIssue(Redeclaration(newImplementation.source, "member", "${typeDefinition.name}.$name",
					existingInterfaceMember.source))
				return
			}
		}
		memberDeclarations.add(newImplementation)
		val signature = "${typeDefinition.name}.$name${newImplementation.signature.toString(false)}"
		linter.addIssue(Declaration(newImplementation.source, "operator", signature))
	}

	override fun resolveValue(name: String): ValueDeclaration? {
		return interfaceMembers[name]
			?: superScope?.resolveValue(name)
			?: enclosingScope.resolveValue(name)
	}

	override fun resolveType(name: String): TypeDefinition? {
		return typeDefinitions[name]
			?: superScope?.resolveType(name)
			?: enclosingScope.resolveType(name)
	}

	fun getGenericTypeDefinitions(): List<GenericTypeDefinition> {
		return typeDefinitions.values.filterIsInstance<GenericTypeDefinition>()
	}

	fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		val conversions = LinkedList<InitializerDefinition>()
		for(initializer in initializers)
			if(initializer.isConvertingFrom(sourceType))
				conversions.add(initializer)
		if(superScope != null)
			conversions.addAll(superScope.getConversionsFrom(sourceType))
		return conversions
	}
}
