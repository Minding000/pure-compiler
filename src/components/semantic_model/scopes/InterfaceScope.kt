package components.semantic_model.scopes

import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.types.Type
import components.semantic_model.values.Instance
import components.semantic_model.values.ValueDeclaration
import java.util.*

//TODO remove all 'getLinkedType' and 'getComputedType' functions in project
class InterfaceScope(val isStatic: Boolean = false): Scope() {
	lateinit var type: Type

	//TODO remove following properties
	val initializers = LinkedList<InitializerDefinition>()
	private val subscribedTypes = LinkedList<Type>()

	fun addSubscriber(type: Type) {
		subscribedTypes.add(type)
		for(initializer in initializers)
			type.onNewInitializer(initializer)
	}

	fun addInitializer(newInitializer: InitializerDefinition) {
		if(!initializers.contains(newInitializer)) {
			initializers.add(newInitializer)
			for(subscriber in subscribedTypes)
				subscriber.onNewInitializer(newInitializer)
		}
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? = type.getTypeDeclaration(name)

	override fun getValueDeclaration(name: String): Pair<ValueDeclaration?, Type?> = type.getValueDeclaration(name)

	fun getSuperInitializer(subInitializer: InitializerDefinition): InitializerDefinition? {
		for(initializer in initializers) {
			if(subInitializer.fulfillsInheritanceRequirementsOf(initializer))
				return initializer
		}
		return null
	}

	fun getAbstractMemberDeclarations() = type.getAbstractMemberDeclarations()
	fun getPropertiesToBeInitialized() = type.getPropertiesToBeInitialized()

	fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return initializers.filter { initializer -> initializer.isConvertingFrom(sourceType) }
	}

	fun hasValueDeclaration(name: String): Boolean {
		val (valueDeclaration) = getValueDeclaration(name)
		return valueDeclaration != null
	}

	fun hasInstance(name: String): Boolean {
		val (valueDeclaration) = getValueDeclaration(name)
		return valueDeclaration is Instance
	}

	override fun toString(): String {
		return "${javaClass.simpleName} of $type"
	}
}
