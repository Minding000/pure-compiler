package components.semantic_model.values

import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.*
import components.semantic_model.scopes.InterfaceScope
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import logger.issues.access.InstanceAccessFromStaticContext
import logger.issues.access.StaticAccessFromInstanceContext
import logger.issues.access.WhereClauseUnfulfilled
import logger.issues.initialization.NotInitialized
import logger.issues.resolution.NotFound
import components.code_generation.llvm.models.values.VariableValue as VariableValueUnit

open class VariableValue(override val source: SyntaxTreeNode, scope: Scope, val name: String): Value(source, scope) {
	var declaration: ValueDeclaration? = null
	var whereClauseConditions: List<WhereClauseCondition>? = null
	protected open var staticType: Type? = null
	override val hasGenericType: Boolean
		get() = (declaration as? Parameter)?.hasGenericType == true
			|| (declaration as? ComputedPropertyDeclaration)?.hasGenericType == true
			|| (declaration?.providedType as? ObjectType)?.getTypeDeclaration() is GenericTypeDeclaration

	constructor(source: Identifier, scope: Scope): this(source, scope, source.getValue())

	override fun determineTypes() {
		super.determineTypes()
		val match = scope.getValueDeclaration(this)
		if(match == null) {
			context.addIssue(NotFound(source, "Value", name))
			return
		}
		val scope = scope
		if(scope is InterfaceScope && match.declaration is InterfaceMember) {
			if(scope.isStatic && !match.declaration.isStatic) {
				context.addIssue(InstanceAccessFromStaticContext(source, name))
				return
			}
			if(!scope.isStatic && match.declaration.isStatic)
				context.addIssue(StaticAccessFromInstanceContext(source, name))
		}
		match.declaration.usages.add(this)
		declaration = match.declaration
		whereClauseConditions = match.whereClauseConditions
		setUnextendedType(match.type)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		val usage = tracker.add(VariableUsage.Kind.READ, this)
		setEndStates(tracker)
		if(usage == null)
			return
		val declaration = declaration
		if(declaration is LocalVariableDeclaration) {
			if(!usage.isPreviouslyInitialized())
				context.addIssue(NotInitialized(source, "Local variable", name))
		} else if(declaration is PropertyDeclaration) {
			if(tracker.isInitializer && !declaration.isStatic && declaration.value == null && !usage.isPreviouslyInitialized()) {
				if(declaration.parentTypeDeclaration == scope.getSurroundingTypeDeclaration())
					context.addIssue(NotInitialized(source, "Property", name))
			}
		}
		computeValue(tracker)
	}

	open fun computeValue(tracker: VariableTracker) {
		staticValue = tracker.getCurrentValueOf(declaration) ?: this
		staticType = tracker.getCurrentTypeOf(declaration)
	}

	override fun getComputedType(): Type? = staticType

	override fun validate() {
		super.validate()
		validateWhereClauseConditions()
	}

	private fun validateWhereClauseConditions() {
		val whereClauseConditions = whereClauseConditions ?: return
		val targetType = (scope as? InterfaceScope)?.type ?: return
		val declaration = declaration as? ComputedPropertyDeclaration ?: return
		val typeParameters = (targetType as? ObjectType)?.typeParameters ?: emptyList()
		for(condition in whereClauseConditions) {
			if(!condition.isMet(typeParameters))
				context.addIssue(WhereClauseUnfulfilled(source, "Computed property", declaration.name, targetType, condition))
		}
	}

	override fun toUnit() = VariableValueUnit(this)

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + (declaration?.hashCode() ?: 0)
		return result
	}

	override fun equals(other: Any?): Boolean {
		if(other !is VariableValue)
			return false
		if(declaration == null)
			return false
		return declaration == other.declaration
	}

	override fun toString(): String = name
}
