package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.semantic_model.declarations.InitializerDefinition
import components.semantic_analysis.semantic_model.declarations.MemberDeclaration
import components.semantic_analysis.semantic_model.declarations.PropertyDeclaration
import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Instance
import components.semantic_analysis.semantic_model.values.InterfaceMember
import java.util.*

class InterfaceScope(val isStatic: Boolean = false): Scope() {
	lateinit var type: Type
	private val typeDeclarations = HashMap<String, TypeDeclaration>()
	private val interfaceMembers = HashMap<String, InterfaceMember>()
	val initializers = LinkedList<InitializerDefinition>()
	private val subscribedTypes = LinkedList<Type>()

	fun addSubscriber(type: Type) {
		subscribedTypes.add(type)
		for((_, typeDeclaration) in typeDeclarations)
			type.onNewTypeDeclaration(typeDeclaration)
		for((_, interfaceMember) in interfaceMembers)
			type.onNewInterfaceMember(interfaceMember)
		for(initializer in initializers)
			type.onNewInitializer(initializer)
	}

	fun addTypeDeclaration(newTypeDeclaration: TypeDeclaration) {
		if(!typeDeclarations.containsKey(newTypeDeclaration.name)) {
			typeDeclarations[newTypeDeclaration.name] = newTypeDeclaration
			for(subscriber in subscribedTypes)
				subscriber.onNewTypeDeclaration(newTypeDeclaration)
		}
	}

	fun addInterfaceMember(newInterfaceMember: InterfaceMember) {
		if(!interfaceMembers.containsKey(newInterfaceMember.name)) {
			interfaceMembers[newInterfaceMember.name] = newInterfaceMember
			for(subscriber in subscribedTypes)
				subscriber.onNewInterfaceMember(newInterfaceMember)
		}
	}

	fun addInitializer(newInitializer: InitializerDefinition) {
		if(!initializers.contains(newInitializer)) {
			initializers.add(newInitializer)
			for(subscriber in subscribedTypes)
				subscriber.onNewInitializer(newInitializer)
		}
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		return typeDeclarations[name]
	}

	override fun getValueDeclaration(name: String): Pair<InterfaceMember?, Type?> {
		val interfaceMember = interfaceMembers[name]
		return Pair(interfaceMember, interfaceMember?.type)
	}

	fun getSuperInitializer(subInitializer: InitializerDefinition): InitializerDefinition? {
		for(initializer in initializers) {
			if(subInitializer.fulfillsInheritanceRequirementsOf(initializer))
				return initializer
		}
		return null
	}

	fun getAbstractMemberDeclarations(): List<MemberDeclaration> = type.getAbstractMemberDeclarations()
	fun getPropertiesToBeInitialized(): List<PropertyDeclaration> = type.getPropertiesToBeInitialized()

	fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return initializers.filter { initializer -> initializer.isConvertingFrom(sourceType) }
	}

	fun hasTypeDeclaration(typeDeclaration: TypeDeclaration): Boolean = typeDeclarations.containsValue(typeDeclaration)

	fun hasInterfaceMember(valueName: String): Boolean = interfaceMembers.containsKey(valueName)
	fun hasInterfaceMember(value: InterfaceMember): Boolean = interfaceMembers.containsValue(value)

	fun hasInstance(name: String): Boolean {
		for((_, value) in interfaceMembers) {
			if(value is Instance && value.name == name)
				return true
		}
		return false
	}

	override fun toString(): String {
		return "${javaClass.simpleName} of $type"
	}
}
