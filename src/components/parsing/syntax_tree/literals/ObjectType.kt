package components.parsing.syntax_tree.literals

import linting.Linter
import linting.semantic_model.types.ObjectType as SemanticObjectTypeModel
import linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.TypeElement

class ObjectType(private val typeList: TypeList?, private val identifier: Identifier):
	TypeElement(identifier.start, typeList?.end ?: identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticObjectTypeModel {
		return SemanticObjectTypeModel(this, identifier.getValue(),
			typeList?.concretizeTypes(linter, scope) ?: listOf())
	}

	override fun toString(): String {
		return "ObjectType { ${if(typeList == null) "" else "$typeList "}$identifier }"
	}
}
