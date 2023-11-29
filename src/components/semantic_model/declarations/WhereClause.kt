package components.semantic_model.declarations

import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.AndUnionType
import components.semantic_model.types.ObjectType
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.semantic_model.values.LocalVariableDeclaration
import components.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.definitions.WhereClause as WhereClauseSyntaxTree

class WhereClause(source: WhereClauseSyntaxTree, scope: TypeScope, val subject: ObjectType, override: Type):
	TypeDeclaration(source, subject.name, scope, null, AndUnionType(source, scope, listOf(subject, override))) {
	override val isDefinition = false

	init {
		scope.typeDeclaration = this
	}

	fun matches(type: Type?): Boolean {
		return (type as? ObjectType)?.getTypeDeclaration() == subject.getTypeDeclaration()
	}

	override fun declare() {
		super.declare()
		scope.enclosingScope.addTypeDeclaration(this)
		scope.enclosingScope.addValueDeclaration(getValueDeclaration())
	}

	override fun getValueDeclaration(): ValueDeclaration {
		val staticType = StaticType(this)
		staticValueDeclaration = LocalVariableDeclaration(source, scope.enclosingScope, name, staticType)
		addSemanticModels(staticValueDeclaration)
		return staticValueDeclaration
	}
}
