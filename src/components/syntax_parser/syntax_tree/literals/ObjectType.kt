package components.syntax_parser.syntax_tree.literals

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeElement
import util.indent
import components.semantic_analysis.semantic_model.types.ObjectType as SemanticObjectTypeModel

class ObjectType(private val enclosingType: ObjectType?, private val typeList: TypeList?, private val identifier: Identifier):
	TypeElement(typeList?.start ?: identifier.start, identifier.end) {

	override fun concretize(scope: MutableScope): SemanticObjectTypeModel {
		val typeList = typeList?.concretizeTypes(scope) ?: listOf()
		return SemanticObjectTypeModel(this, scope, enclosingType?.concretize(scope), typeList, identifier.getValue())
	}

	override fun toString(): String {
		var stringRepresentation = "ObjectType"
		if(enclosingType != null)
			stringRepresentation += " [${"\n$enclosingType".indent()}\n]"
		stringRepresentation += " { "
		if(typeList != null)
			stringRepresentation += "$typeList "
		stringRepresentation += "$identifier }"
		return stringRepresentation
	}
}
