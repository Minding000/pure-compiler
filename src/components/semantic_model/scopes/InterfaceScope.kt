package components.semantic_model.scopes

import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.declarations.Instance
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.types.Type

class InterfaceScope(val isStatic: Boolean = false): Scope() {
	lateinit var type: Type

	fun getDirectInitializers(): List<InitializerDefinition> = type.getInitializers()
	fun getAllInitializers(): List<InitializerDefinition> = type.getAllInitializers()
	override fun getTypeDeclaration(name: String): TypeDeclaration? = type.getTypeDeclaration(name)
	override fun getValueDeclaration(name: String): ValueDeclaration.Match? = type.getValueDeclaration(name)

	fun getSuperInitializer(subInitializer: InitializerDefinition): InitializerDefinition? {
		for(initializer in getAllInitializers()) {
			if(subInitializer.fulfillsInheritanceRequirementsOf(initializer))
				return initializer
		}
		return null
	}

	fun getPropertiesToBeInitialized() = type.getPropertiesToBeInitialized()

	fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return getDirectInitializers().filter { initializer -> initializer.isConvertingFrom(sourceType) }
	}

	fun hasValueDeclaration(name: String): Boolean {
		return getValueDeclaration(name) != null
	}

	fun hasInstance(name: String): Boolean {
		return getValueDeclaration(name)?.declaration is Instance
	}

	override fun toString(): String {
		return "${javaClass.simpleName} of $type"
	}
}
