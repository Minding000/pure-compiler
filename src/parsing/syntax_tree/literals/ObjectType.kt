package parsing.syntax_tree.literals

import linting.Linter
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.TypeElement

class ObjectType(private val typeList: TypeList?, private val identifier: Identifier):
	TypeElement(identifier.start, typeList?.end ?: identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): ObjectType {
		return ObjectType(
			this, identifier.getValue(),
			typeList?.concretizeTypes(linter, scope) ?: listOf()
		)
	}

	override fun toString(): String {
		return "ObjectType { ${if(typeList == null) "" else "$typeList "}$identifier }"
	}
}