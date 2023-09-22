package components.semantic_model.values

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.PropertyDeclaration
import components.semantic_model.scopes.InterfaceScope
import components.semantic_model.scopes.Scope
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import logger.issues.access.InstanceAccessFromStaticContext
import logger.issues.access.StaticAccessFromInstanceContext
import logger.issues.initialization.NotInitialized
import logger.issues.resolution.NotFound

open class VariableValue(override val source: SyntaxTreeNode, scope: Scope, val name: String): Value(source, scope) {
	var declaration: ValueDeclaration? = null
	protected open var staticType: Type? = null

	constructor(source: Identifier, scope: Scope): this(source, scope, source.getValue())

	override fun determineTypes() {
		super.determineTypes()
		val (valueDeclaration, type) = scope.getValueDeclaration(this)
		if(valueDeclaration == null) {
			context.addIssue(NotFound(source, "Value", name))
			return
		}
		val scope = scope
		if(scope is InterfaceScope && valueDeclaration is InterfaceMember) {
			if(scope.isStatic && !valueDeclaration.isStatic) {
				context.addIssue(InstanceAccessFromStaticContext(source, name))
				return
			}
			if(!scope.isStatic && valueDeclaration.isStatic)
				context.addIssue(StaticAccessFromInstanceContext(source, name))
		}
		valueDeclaration.usages.add(this)
		this.declaration = valueDeclaration
		this.type = type
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		val usage = tracker.add(VariableUsage.Kind.READ, this)
		setEndStates(tracker)
		if(usage == null)
			return
		val declaration = declaration
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
		staticValue = tracker.getCurrentValueOf(declaration) ?: this
		staticType = tracker.getCurrentTypeOf(declaration)
	}

	override fun getComputedType(): Type? = staticType

	override fun getLlvmLocation(constructor: LlvmConstructor): LlvmValue? {
		val definition = declaration
		return if(definition is PropertyDeclaration) {
			context.resolveMember(constructor, definition.parentTypeDeclaration.llvmType, context.getThisParameter(constructor), name,
				(definition as? InterfaceMember)?.isStatic ?: false)
		} else {
			definition?.llvmLocation
		}
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		return constructor.buildLoad(type?.getLlvmType(constructor), getLlvmLocation(constructor), name)
	}

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
