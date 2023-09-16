package components.syntax_parser.syntax_tree.literals

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import util.indent
import components.semantic_model.types.ObjectType as SemanticObjectTypeModel

class ObjectType(private val enclosingType: ObjectType?, private val typeList: TypeList?, private val identifier: Identifier):
	TypeSyntaxTreeNode(typeList?.start ?: identifier.start, identifier.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticObjectTypeModel {
		val typeList = typeList?.toSemanticModels(scope) ?: emptyList()
		return SemanticObjectTypeModel(this, scope, enclosingType?.toSemanticModel(scope), typeList, identifier.getValue())
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
