package components.syntax_parser.syntax_tree.literals

import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.SelfType
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import logger.issues.declaration.InvalidSelfTypeLocation
import util.indent
import components.semantic_model.types.ObjectType as SemanticObjectTypeModel

class ObjectType(private val enclosingType: ObjectType?, private val typeList: TypeList?, private val identifier: Identifier):
	TypeSyntaxTreeNode(typeList?.start ?: identifier.start, identifier.end) {

	companion object {
		const val SELF_TYPE_NAME = "Self"
	}

	override fun toSemanticModel(scope: MutableScope): Type {
		val typeList = typeList?.toSemanticModels(scope) ?: emptyList()
		val name = identifier.getValue()
		if(name == SELF_TYPE_NAME) {
			//TODO disallow non-empty type list
			//TODO disallow enclosing type
			return SelfType(this, scope)
		}
		return SemanticObjectTypeModel(this, scope, enclosingType?.toSemanticObjectTypeModel(scope), typeList, name)
	}


	fun toSemanticObjectTypeModel(scope: MutableScope): SemanticObjectTypeModel? {
		val typeList = typeList?.toSemanticModels(scope) ?: emptyList()
		val name = identifier.getValue()
		if(name == SELF_TYPE_NAME) {
			//TODO error message could be more precise (may be inside of class)
			context.addIssue(InvalidSelfTypeLocation(this))
			return null
		}
		return SemanticObjectTypeModel(this, scope, enclosingType?.toSemanticObjectTypeModel(scope), typeList, name)
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
