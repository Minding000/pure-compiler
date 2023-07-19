package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.semantic_model.definitions.*
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import errors.internal.CompilerError
import logger.issues.definition.*
import java.util.*

class TypeScope(val enclosingScope: MutableScope, private val superScope: InterfaceScope?): MutableScope() {
	lateinit var typeDefinition: TypeDefinition
	val typeDefinitions = LinkedHashMap<String, TypeDefinition>()
	val memberDeclarations = LinkedList<MemberDeclaration>()
	private val interfaceMembers = HashMap<String, InterfaceMember>()
	val initializers = LinkedList<InitializerDefinition>()

	fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>, superScope: InterfaceScope?): TypeScope {
		val specificTypeScope = TypeScope(enclosingScope, superScope)
		for((name, typeDefinition) in typeDefinitions) {
			if(typeDefinition is GenericTypeDefinition)
				continue
			if(typeDefinition.isBound) {
				typeDefinition.withTypeSubstitutions(typeSubstitution) { specificDefinition ->
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
				interfaceMember.withTypeSubstitutions(typeSubstitution)
		}
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
			!member.isStatic && member.type !is StaticType && !member.isAbstract && member.value == null })
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

	fun ensureTrivialInitializers() {
		for(initializer in initializers) {
			if(initializer.typeParameters.isNotEmpty())
				initializer.context.addIssue(ObjectInitializerTakingTypeParameters(initializer.source))
			if(initializer.parameters.isNotEmpty())
				initializer.context.addIssue(ObjectInitializerTakingParameters(initializer.source))
		}
	}

	fun ensureUniqueInitializerSignatures() {
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
				otherInitializer.context.addIssue(Redeclaration(otherInitializer.source, "initializer", otherInitializer.toString(),
					initializer.source))
			}
		}
		initializers.removeAll(redeclarations)
	}

	fun ensureNoAbstractMembers() {
		for(memberDeclaration in memberDeclarations) {
			if(memberDeclaration.isAbstract)
				typeDefinition.context.addIssue(AbstractMemberInNonAbstractTypeDefinition(memberDeclaration, typeDefinition))
		}
	}

	fun ensureAbstractSuperMembersImplemented() {
		val missingOverrides = LinkedHashMap<TypeDefinition, LinkedList<MemberDeclaration>>()
		val abstractSuperMembers = superScope?.getAbstractMembers() ?: return
		for(abstractSuperMember in abstractSuperMembers) {
			val overridingMember = memberDeclarations.find { memberDeclaration ->
				memberDeclaration.memberIdentifier == abstractSuperMember.memberIdentifier }
			if(!abstractSuperMember.canBeOverriddenBy(overridingMember)) {
				val parentDefinition = abstractSuperMember.parentDefinition
					?: throw CompilerError(abstractSuperMember.source, "Member is missing parent definition.")
				val missingOverridesFromType = missingOverrides.getOrPut(parentDefinition) { LinkedList() }
				missingOverridesFromType.add(abstractSuperMember)
			}
		}
		if(missingOverrides.isEmpty())
			return
		typeDefinition.context.addIssue(MissingImplementations(typeDefinition, missingOverrides))
	}

	override fun declareInitializer(initializer: InitializerDefinition) {
		initializers.add(initializer)
		onNewInitializer(initializer)
		memberDeclarations.add(initializer)
	}

	override fun declareType(typeDefinition: TypeDefinition) {
		var previousDeclaration = enclosingScope.resolveType(typeDefinition.name)
		if(previousDeclaration != null)
			typeDefinition.context.addIssue(ShadowsElement(typeDefinition.source, "type", typeDefinition.name,
				previousDeclaration.source))
		previousDeclaration = superScope?.resolveType(typeDefinition.name)
			?: typeDefinitions.putIfAbsent(typeDefinition.name, typeDefinition)
		if(previousDeclaration != null) {
			typeDefinition.context.addIssue(Redeclaration(typeDefinition.source, "type",
				"${this.typeDefinition.name}.${typeDefinition.name}", previousDeclaration.source))
			return
		}
		onNewType(typeDefinition)
	}

	override fun declareValue(valueDeclaration: ValueDeclaration) {
		if(valueDeclaration !is InterfaceMember)
			throw CompilerError(valueDeclaration.source,
				"Tried to declare non-member of type '${valueDeclaration.javaClass.simpleName}' in type scope.")
		var previousDeclaration = enclosingScope.resolveValue(valueDeclaration.name)
		if(previousDeclaration != null)
			valueDeclaration.context.addIssue(ShadowsElement(valueDeclaration.source, "member", valueDeclaration.name,
				previousDeclaration.source))
		previousDeclaration = superScope?.resolveValue(valueDeclaration.name) ?: interfaceMembers.putIfAbsent(valueDeclaration.name,
			valueDeclaration)
		if(previousDeclaration != null) {
			valueDeclaration.context.addIssue(Redeclaration(valueDeclaration.source, "member",
				"${typeDefinition.name}.${valueDeclaration.name}", previousDeclaration.source))
			return
		}
		val value = valueDeclaration.value
		if(value is Function)
			memberDeclarations.addAll(value.implementations)
		else
			memberDeclarations.add(valueDeclaration)
		onNewValue(valueDeclaration)
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
