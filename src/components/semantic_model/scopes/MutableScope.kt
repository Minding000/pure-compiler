package components.semantic_model.scopes

import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.values.ValueDeclaration

abstract class MutableScope: Scope() {

	abstract fun addTypeDeclaration(newTypeDeclaration: TypeDeclaration)

	abstract fun addValueDeclaration(newValueDeclaration: ValueDeclaration)
}
