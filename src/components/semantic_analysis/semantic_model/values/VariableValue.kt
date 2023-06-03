package components.semantic_analysis.semantic_model.values

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.context.VariableUsage
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.scopes.InterfaceScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import logger.issues.access.InstanceAccessFromStaticContext
import logger.issues.access.StaticAccessFromInstanceContext
import logger.issues.initialization.NotInitialized
import logger.issues.resolution.NotFound

open class VariableValue(override val source: SyntaxTreeNode, scope: Scope, val name: String): Value(source, scope) {
	var definition: ValueDeclaration? = null
	protected open var staticType: Type? = null

	constructor(source: Identifier, scope: Scope): this(source, scope, source.getValue())

	override fun determineTypes() {
		super.determineTypes()
		val definition = scope.resolveValue(this)
		if(definition == null) {
			context.addIssue(NotFound(source, "Value", name))
			return
		}
		val scope = scope
		if(scope is InterfaceScope && definition is InterfaceMember) {
			if(scope.isStatic && !definition.isStatic) {
				context.addIssue(InstanceAccessFromStaticContext(source, name))
				return
			}
			if(!scope.isStatic && definition.isStatic)
				context.addIssue(StaticAccessFromInstanceContext(source, name))
		}
		definition.usages.add(this)
		this.definition = definition
		type = definition.getLinkedType()
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		val usage = tracker.add(VariableUsage.Kind.READ, this)
		setEndStates(tracker)
		if(usage == null)
			return
		val declaration = definition
		if(declaration is LocalVariableDeclaration) {
			if(declaration.type !is StaticType && !usage.isPreviouslyInitialized())
				context.addIssue(NotInitialized(source, "Local variable", name))
		} else if(declaration is PropertyDeclaration) {
			if(tracker.isInitializer && !declaration.isStatic && declaration.value == null && !usage.isPreviouslyInitialized())
				context.addIssue(NotInitialized(source, "Property", name))
		}
		computeValue(tracker)
	}

	open fun computeValue(tracker: VariableTracker) {
		staticValue = tracker.getCurrentValueOf(definition) ?: this
		staticType = tracker.getCurrentTypeOf(definition)
	}

	override fun getComputedType(): Type? = staticType

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

	override fun getLlvmReference(llvmConstructor: LlvmConstructor): LlvmValue {
		val location = definition?.llvmLocation
		return llvmConstructor.buildLoad(location, name)
	}

	override fun toString(): String = name
}
