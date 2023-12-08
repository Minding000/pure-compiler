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

class WhereClauseCondition(override val source: WhereClauseConditionSyntaxTree, scope: TypeScope, val subject: Type, val override: Type,
						   private val isOriginal: Boolean = true):
	TypeDeclaration(source, (subject as? ObjectType)?.name ?: "", scope, null,
		AndUnionType(source, scope, listOf(subject, override))) {
	override val isDefinition = false
	private var subjectTypeDeclarationIndex = NOT_FOUND

	init {
		scope.typeDeclaration = this
	}

	fun isMet(typeParameters: List<Type>): Boolean {
		val subjectTypeDefinition = getSubjectTypeDefinition()
		if(subjectTypeDefinition is GenericTypeDeclaration) {
			val typeParameter = typeParameters.getOrNull(subjectTypeDeclarationIndex) ?: return false
			return override.accepts(typeParameter)
		}
		return override.accepts(subject)
	}

	fun matches(type: Type?): Boolean {
		return (type as? ObjectType)?.getTypeDeclaration() == getSubjectTypeDefinition()
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
		val surroundingTypeDefinition = parent?.scope?.getSurroundingTypeDeclaration()
		val subjectTypeDefinition = getSubjectTypeDefinition()
		if(surroundingTypeDefinition != null && (isOriginal || subjectTypeDefinition is GenericTypeDeclaration)) {
			subjectTypeDeclarationIndex = surroundingTypeDefinition.scope.getGenericTypeDeclarations().indexOf(subjectTypeDefinition)
			if(subjectTypeDeclarationIndex == NOT_FOUND)
				context.addIssue(InvalidWhereClauseSubject(this, surroundingTypeDefinition.name))
		}
	}

	fun getSubjectTypeDefinition(): TypeDeclaration? {
		return (subject as? ObjectType)?.getTypeDeclaration()
	}

	fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): WhereClauseCondition {
		return WhereClauseCondition(source, scope, subject.withTypeSubstitutions(typeSubstitutions), override, false)
	}

	override fun toString(): String = "${source.subject.getValue()} is $override"
}
