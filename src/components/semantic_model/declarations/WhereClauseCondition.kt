package components.semantic_model.declarations

import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.AndUnionType
import components.semantic_model.types.ObjectType
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.semantic_model.values.LocalVariableDeclaration
import logger.issues.declaration.InvalidWhereClauseSubject
import util.NOT_FOUND
import components.syntax_parser.syntax_tree.definitions.WhereClauseCondition as WhereClauseConditionSyntaxTree

class WhereClauseCondition(source: WhereClauseConditionSyntaxTree, scope: TypeScope, val subject: ObjectType, val override: Type):
	TypeDeclaration(source, subject.name, scope, null, AndUnionType(source, scope, listOf(subject, override))) {
	override val isDefinition = false
	var subjectTypeDeclarationIndex = NOT_FOUND

	init {
		scope.typeDeclaration = this
	}

	fun matches(type: Type?): Boolean {
		return (type as? ObjectType)?.getTypeDeclaration() == subject.getTypeDeclaration()
	}

	override fun declare() {
		super.declare()
		val staticType = StaticType(this)
		val parent = parent
		if(parent is ComputedPropertyDeclaration) {
			if(parent.getterErrorHandlingContext != null) {
				parent.getterScope.addTypeDeclaration(this)
				staticValueDeclaration = LocalVariableDeclaration(source, parent.getterScope, name, staticType)
				staticValueDeclaration.declare()
				addSemanticModels(staticValueDeclaration)
			}
			if(parent.setterErrorHandlingContext != null) {
				parent.setterScope.addTypeDeclaration(this)
				staticValueDeclaration = LocalVariableDeclaration(source, parent.setterScope, name, staticType)
				staticValueDeclaration.declare()
				addSemanticModels(staticValueDeclaration)
			}
		} else {
			scope.enclosingScope.addTypeDeclaration(this)
			staticValueDeclaration = LocalVariableDeclaration(source, scope.enclosingScope, name, staticType)
			staticValueDeclaration.declare()
			addSemanticModels(staticValueDeclaration)
		}
	}

	override fun determineTypes() {
		super.determineTypes()
		val typeDefinition = parent?.scope?.getSurroundingTypeDeclaration()
		if(typeDefinition != null) {
			subjectTypeDeclarationIndex = typeDefinition.scope.getGenericTypeDeclarations().indexOf(subject.getTypeDeclaration())
			if(subjectTypeDeclarationIndex == NOT_FOUND)
				context.addIssue(InvalidWhereClauseSubject(this, typeDefinition.name))
		}
	}

	override fun toString(): String = "$subject is $override"
}
