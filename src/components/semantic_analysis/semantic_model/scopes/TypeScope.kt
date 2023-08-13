package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.semantic_model.declarations.*
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import errors.internal.CompilerError
import logger.issues.declaration.*
import java.util.*

class TypeScope(val enclosingScope: MutableScope, private val superScope: InterfaceScope?): MutableScope() {
	lateinit var typeDeclaration: TypeDeclaration
	val typeDeclarations = LinkedHashMap<String, TypeDeclaration>()
	val memberDeclarations = LinkedList<MemberDeclaration>()
	private val interfaceMembers = HashMap<String, InterfaceMember>()
	val initializers = LinkedList<InitializerDefinition>()
	private val subscribedTypes = LinkedList<Type>()

	fun withTypeSubstitutions(typeSubstitution: Map<TypeDeclaration, Type>, superScope: InterfaceScope?): TypeScope {
		val specificTypeScope = TypeScope(enclosingScope, superScope)
		for((name, typeDeclaration) in typeDeclarations) {
			if(typeDeclaration is GenericTypeDeclaration)
				continue
			if(typeDeclaration.isBound) {
				typeDeclaration.withTypeSubstitutions(typeSubstitution) { specificDefinition ->
					specificTypeScope.typeDeclarations[name] = specificDefinition
				}
			} else {
				specificTypeScope.typeDeclarations[name] = typeDeclaration
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

	fun addSubscriber(type: Type) {
		subscribedTypes.add(type)
		for((_, typeDeclaration) in typeDeclarations)
			type.onNewTypeDeclaration(typeDeclaration)
		for((_, interfaceMember) in interfaceMembers)
			type.onNewInterfaceMember(interfaceMember)
		for(initializer in initializers)
			type.onNewInitializer(initializer)
		if(type !is StaticType)
			superScope?.addSubscriber(type)
	}

	fun getAbstractMemberDeclarations(): List<MemberDeclaration> {
		val abstractMemberDeclarations = LinkedList<MemberDeclaration>()
		if(superScope != null)
			abstractMemberDeclarations.addAll(superScope.getAbstractMemberDeclarations())
		for(memberDeclaration in memberDeclarations) {
			if(memberDeclaration.isAbstract)
				abstractMemberDeclarations.add(memberDeclaration)
		}
		return abstractMemberDeclarations
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
			val superMember = superScope?.getValueDeclaration(interfaceMember.name) ?: continue
			interfaceMember.superMember = superMember
			val function = interfaceMember.value as? Function ?: continue
			function.functionType.superFunctionType = superMember.type as? FunctionType
		}
	}

	fun ensureTrivialInitializers() {
		for(initializer in initializers) {
			if(initializer.localTypeParameters.isNotEmpty())
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
				typeDeclaration.context.addIssue(AbstractMemberInNonAbstractTypeDefinition(memberDeclaration, typeDeclaration))
		}
	}

	fun ensureAbstractSuperMembersImplemented() {
		val missingOverrides = LinkedHashMap<TypeDeclaration, LinkedList<MemberDeclaration>>()
		val abstractSuperMembers = superScope?.getAbstractMemberDeclarations() ?: return
		for(abstractSuperMember in abstractSuperMembers) {
			val overridingMember = memberDeclarations.find { memberDeclaration ->
				memberDeclaration.memberIdentifier == abstractSuperMember.memberIdentifier }
			if(!abstractSuperMember.canBeOverriddenBy(overridingMember)) {
				val parentDefinition = abstractSuperMember.parentTypeDeclaration
					?: throw CompilerError(abstractSuperMember.source, "Member is missing parent definition.")
				val missingOverridesFromType = missingOverrides.getOrPut(parentDefinition) { LinkedList() }
				missingOverridesFromType.add(abstractSuperMember)
			}
		}
		if(missingOverrides.isEmpty())
			return
		typeDeclaration.context.addIssue(MissingImplementations(typeDeclaration, missingOverrides))
	}

	fun addInitializer(newInitializer: InitializerDefinition) {
		initializers.add(newInitializer)
		memberDeclarations.add(newInitializer)
		for(subscriber in subscribedTypes)
			subscriber.onNewInitializer(newInitializer)
	}

	override fun addTypeDeclaration(newTypeDeclaration: TypeDeclaration) {
		var existingTypeDeclaration = enclosingScope.getTypeDeclaration(newTypeDeclaration.name)
		if(existingTypeDeclaration != null)
			newTypeDeclaration.context.addIssue(ShadowsElement(newTypeDeclaration.source, "type", newTypeDeclaration.name,
				existingTypeDeclaration.source))
		existingTypeDeclaration = superScope?.getTypeDeclaration(newTypeDeclaration.name)
			?: typeDeclarations.putIfAbsent(newTypeDeclaration.name, newTypeDeclaration)
		if(existingTypeDeclaration != null) {
			newTypeDeclaration.context.addIssue(Redeclaration(newTypeDeclaration.source, "type",
				"${this.typeDeclaration.name}.${newTypeDeclaration.name}", existingTypeDeclaration.source))
			return
		}
		for(subscriber in subscribedTypes)
			subscriber.onNewTypeDeclaration(newTypeDeclaration)
	}

	override fun addValueDeclaration(newValueDeclaration: ValueDeclaration) {
		if(newValueDeclaration !is InterfaceMember)
			throw CompilerError(newValueDeclaration.source,
				"Tried to declare non-member of type '${newValueDeclaration.javaClass.simpleName}' in type scope.")
		var existingValueDeclaration = enclosingScope.getValueDeclaration(newValueDeclaration.name)
		if(existingValueDeclaration != null)
			newValueDeclaration.context.addIssue(ShadowsElement(newValueDeclaration.source, "member", newValueDeclaration.name,
				existingValueDeclaration.source))
		existingValueDeclaration = superScope?.getValueDeclaration(newValueDeclaration.name)
			?: interfaceMembers.putIfAbsent(newValueDeclaration.name, newValueDeclaration)
		if(existingValueDeclaration != null) {
			newValueDeclaration.context.addIssue(Redeclaration(newValueDeclaration.source, "member",
				"${typeDeclaration.name}.${newValueDeclaration.name}", existingValueDeclaration.source))
			return
		}
		val value = newValueDeclaration.value
		if(value is Function)
			memberDeclarations.addAll(value.implementations)
		else
			memberDeclarations.add(newValueDeclaration)
		for(subscriber in subscribedTypes)
			subscriber.onNewInterfaceMember(newValueDeclaration)
	}

	override fun getValueDeclaration(name: String): ValueDeclaration? {
		return interfaceMembers[name]
			?: superScope?.getValueDeclaration(name)
			?: enclosingScope.getValueDeclaration(name)
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		return typeDeclarations[name]
			?: superScope?.getTypeDeclaration(name)
			?: enclosingScope.getTypeDeclaration(name)
	}

	override fun getSurroundingTypeDeclaration(): TypeDeclaration {
		return typeDeclaration
	}

	fun getGenericTypeDeclarations(): List<GenericTypeDeclaration> {
		return typeDeclarations.values.filterIsInstance<GenericTypeDeclaration>()
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
