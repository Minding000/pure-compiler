package components.semantic_model.scopes

import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.types.Type
import components.semantic_model.values.ValueDeclaration
import logger.issues.declaration.Redeclaration

class FileScope: MutableScope() {
	private val referencedTypeDeclarations = HashMap<String, TypeDeclaration>()
	val typeDeclarations = HashMap<String, TypeDeclaration>()
	private val referencedValueDeclarations = HashMap<String, ValueDeclaration>()
	private val valueDeclarations = HashMap<String, ValueDeclaration>()

	fun reference(scope: FileScope, referenceAliases: Map<String, String>) {
		for((originalName, typeDeclaration) in scope.typeDeclarations) {
			val localName = referenceAliases[originalName] ?: originalName
			referencedTypeDeclarations[localName] = typeDeclaration
		}
		for((originalName, valueDeclaration) in scope.valueDeclarations) {
			val localName = referenceAliases[originalName] ?: originalName
			referencedValueDeclarations[localName] = valueDeclaration
		}
	}

	override fun addTypeDeclaration(newTypeDeclaration: TypeDeclaration) {
		val existingTypeDeclaration = typeDeclarations.putIfAbsent(newTypeDeclaration.name, newTypeDeclaration)
		if(existingTypeDeclaration != null)
			newTypeDeclaration.context.addIssue(Redeclaration(newTypeDeclaration.source, "type", newTypeDeclaration.name,
				existingTypeDeclaration.source))
	}

	override fun getTypeDeclaration(name: String): TypeDeclaration? {
		return typeDeclarations[name] ?: referencedTypeDeclarations[name]
	}

	override fun addValueDeclaration(newValueDeclaration: ValueDeclaration) {
		val existingValueDeclaration = valueDeclarations.putIfAbsent(newValueDeclaration.name, newValueDeclaration)
		if(existingValueDeclaration != null)
			newValueDeclaration.context.addIssue(Redeclaration(newValueDeclaration.source, "value", newValueDeclaration.name,
				existingValueDeclaration.source))
	}

	override fun getValueDeclaration(name: String): Pair<ValueDeclaration?, Type?> {
		val valueDeclaration = valueDeclarations[name] ?: referencedValueDeclarations[name]
		return Pair(valueDeclaration, valueDeclaration?.type)
	}
}
