package parsing.ast.literals

import linter.Linter
import linter.elements.literals.ObjectType
import linter.scopes.MutableScope
import parsing.ast.general.TypeElement

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