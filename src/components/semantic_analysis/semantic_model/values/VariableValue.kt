package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.scopes.InterfaceScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.StaticType
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.literals.Identifier
import logger.issues.access.InstanceAccessFromStaticContext
import logger.issues.access.StaticAccessFromInstanceContext
import logger.issues.initialization.NotInitialized
import logger.issues.resolution.NotFound

open class VariableValue(override val source: Element, scope: Scope, val name: String): Value(source, scope) {
	var definition: ValueDeclaration? = null

	constructor(source: Identifier, scope: Scope): this(source, scope, source.getValue())

	override fun linkValues(linter: Linter) {
		if(definition != null)
			return
		val definition = scope.resolveValue(this)
		if(definition == null) {
			linter.addIssue(NotFound(source, "Value", name))
			return
		}
		val scope = scope
		if(scope is InterfaceScope && definition is InterfaceMember) {
			if(scope.isStatic && !definition.isStatic) {
				linter.addIssue(InstanceAccessFromStaticContext(source, name))
				return
			}
			if(!scope.isStatic && definition.isStatic)
				linter.addIssue(StaticAccessFromInstanceContext(source, name))
		}
		definition.usages.add(this)
		this.definition = definition
		definition.preLinkValues(linter)
		type = definition.type
		if(definition.isConstant)
			staticValue = definition.value?.staticValue
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		val usage = tracker.add(VariableUsage.Kind.READ, this)
		setEndStates(tracker)
		if(usage == null)
			return
		val declaration = definition
		if(declaration is LocalVariableDeclaration) {
			if(declaration.type !is StaticType && !usage.isPreviouslyInitialized())
				tracker.linter.addIssue(NotInitialized(source, "Local variable", name))
		} else if(declaration is PropertyDeclaration) {
			if(tracker.isInitializer && !declaration.isStatic && declaration.value == null && !usage.isPreviouslyInitialized())
				tracker.linter.addIssue(NotInitialized(source, "Property", name))
		}
	}

	override fun getComputedValue(tracker: VariableTracker): Value? {
		return tracker.getCurrentValueOf(definition)
	}

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + (definition?.hashCode() ?: 0)
		return result
	}

	override fun equals(other: Any?): Boolean {
		if(other !is VariableValue)
			return false
		if(definition == null)
			return false
		return definition == other.definition
	}

	override fun toString(): String = name
}
