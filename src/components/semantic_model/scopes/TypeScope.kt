package components.semantic_model.scopes

import components.semantic_model.declarations.*
import components.semantic_model.types.FunctionType
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.semantic_model.values.Function
import components.semantic_model.values.InterfaceMember
import components.semantic_model.values.ValueDeclaration
import errors.internal.CompilerError
import logger.issues.declaration.AbstractMemberInNonAbstractTypeDefinition
import logger.issues.declaration.ObjectInitializerTakingParameters
import logger.issues.declaration.ObjectInitializerTakingTypeParameters
import logger.issues.declaration.Redeclaration
import java.util.*

class TypeScope(val enclosingScope: MutableScope): MutableScope() {
	var superScope: InterfaceScope? = null
	lateinit var typeDeclaration: TypeDeclaration
	val typeDeclarations = LinkedHashMap<String, TypeDeclaration>()
	val memberDeclarations = LinkedList<MemberDeclaration>()
	private val interfaceMembers = HashMap<String, InterfaceMember>()
	val initializers = LinkedList<InitializerDefinition>()

	fun getAbstractMemberDeclarations(): List<MemberDeclaration> {
		val abstractMemberDeclarations = LinkedList<MemberDeclaration>()
		for(memberDeclaration in memberDeclarations) {
			if(memberDeclaration.isAbstract)
				abstractMemberDeclarations.add(memberDeclaration)
		}
		return abstractMemberDeclarations
	}

	fun getSpecificMemberDeclarations(): List<MemberDeclaration> {
		val specificMemberDeclarations = LinkedList<MemberDeclaration>()
		for(memberDeclaration in memberDeclarations) {
			if(memberDeclaration is FunctionImplementation && memberDeclaration.isSpecific)
				specificMemberDeclarations.add(memberDeclaration)
		}
		return specificMemberDeclarations
	}

	fun getPropertiesToBeInitialized(): List<PropertyDeclaration> {
		val propertiesToBeInitialized = LinkedList<PropertyDeclaration>()
		val superScope = superScope
		if(superScope != null)
			propertiesToBeInitialized.addAll(superScope.getPropertiesToBeInitialized())
		propertiesToBeInitialized.addAll(memberDeclarations.filterIsInstance<PropertyDeclaration>().filter { member ->
			!member.isStatic && member.type !is StaticType && !member.isAbstract && member.value == null })
		return propertiesToBeInitialized
	}

	fun inheritSignatures() {
		//TODO do this on-demand when resolving initializers to avoid out-of-order-resolution
		for(initializer in initializers)
			initializer.superInitializer = superScope?.getSuperInitializer(initializer)
		for((_, interfaceMember) in interfaceMembers) {
			if(interfaceMember.type is StaticType)
				continue
			val (superMember, _, superMemberType) = superScope?.getValueDeclaration(interfaceMember.name) ?: continue
			val superInterfaceMember = superMember as? InterfaceMember ?: continue
			interfaceMember.superMember = Pair(superInterfaceMember, superMemberType)
			val functionType = interfaceMember.type as? FunctionType ?: continue
			functionType.determineSuperType()
			functionType.determineSuperSignatures()
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

	fun addInitializer(newInitializer: InitializerDefinition) {
		initializers.add(newInitializer)
		memberDeclarations.add(newInitializer)
	}

	override fun addTypeDeclaration(newTypeDeclaration: TypeDeclaration) {
		val existingTypeDeclaration = typeDeclarations.putIfAbsent(newTypeDeclaration.name, newTypeDeclaration)
		if(existingTypeDeclaration != null)
			newTypeDeclaration.context.addIssue(Redeclaration(newTypeDeclaration.source, "type",
				"${this.typeDeclaration.name}.${newTypeDeclaration.name}", existingTypeDeclaration.source))
	}

	override fun addValueDeclaration(newValueDeclaration: ValueDeclaration) {
		if(newValueDeclaration !is InterfaceMember)
			throw CompilerError(newValueDeclaration.source,
				"Tried to declare non-member of type '${newValueDeclaration.javaClass.simpleName}' in type scope.")
		val existingValueDeclaration = interfaceMembers.putIfAbsent(newValueDeclaration.name, newValueDeclaration)
		if(existingValueDeclaration != null) {
			newValueDeclaration.context.addIssue(Redeclaration(newValueDeclaration.source, "member",
				"${typeDeclaration.name}.${newValueDeclaration.name}", existingValueDeclaration.source))
			return
		}
		val value = newValueDeclaration.value
		if(value is Function) {
			value.associatedTypeDeclaration = typeDeclaration
			memberDeclarations.addAll(value.implementations)
		} else {
			memberDeclarations.add(newValueDeclaration)
		}
	}

	override fun getValueDeclaration(name: String): ValueDeclaration.Match? {
		val interfaceMember = interfaceMembers[name]
			?: return superScope?.getValueDeclaration(name)
				?: enclosingScope.getValueDeclaration(name)
		return ValueDeclaration.Match(interfaceMember)
	}

	fun getDirectValueDeclaration(name: String): ValueDeclaration.Match? {
		val interfaceMember = interfaceMembers[name] ?: return superScope?.getValueDeclaration(name)
		return ValueDeclaration.Match(interfaceMember)
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		return typeDeclarations[name]
			?: superScope?.getTypeDeclaration(name)
			?: enclosingScope.getTypeDeclaration(name)
	}

	fun getDirectTypeDeclaration(name: String): TypeDeclaration? {
		return typeDeclarations[name]
			?: superScope?.getTypeDeclaration(name)
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
		val superScope = superScope
		if(superScope != null)
			conversions.addAll(superScope.getConversionsFrom(sourceType))
		return conversions
	}
}
