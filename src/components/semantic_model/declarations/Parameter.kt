package components.semantic_model.declarations

import components.code_generation.llvm.LlvmConstructor
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.Type
import components.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.declaration.PropertyParameterMismatch
import logger.issues.declaration.PropertyParameterOutsideOfInitializer
import kotlin.properties.Delegates

class Parameter(override val source: SyntaxTreeNode, scope: MutableScope, name: String, type: Type?, isMutable: Boolean = false,
				val isVariadic: Boolean = false):
	ValueDeclaration(source, scope, name, type, null, true, isMutable) {
	val isPropertySetter = type == null
	var propertyDeclaration: ValueDeclaration? = null
	var index by Delegates.notNull<Int>()

	override fun declare() {
		if(type != null)
			scope.addValueDeclaration(this)
	}

	override fun determineType() {
		if(isPropertySetter) {
			val parent = parent
			if(parent is InitializerDefinition) {
				//TODO disallow computed property with where clause as target
				val (propertyDeclaration, _, type) = parent.parentTypeDeclaration.scope.getValueDeclaration(name)
				this.propertyDeclaration = propertyDeclaration
				this.type = type
				if(propertyDeclaration == null)
					context.addIssue(PropertyParameterMismatch(source))
			} else {
				context.addIssue(PropertyParameterOutsideOfInitializer(source))
			}
		}
		super.determineType()
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(isPropertySetter) {
			propertyDeclaration?.let { propertyDeclaration ->
				tracker.add(VariableUsage.Kind.WRITE, propertyDeclaration, this)
			}
		} else {
			tracker.declare(this, true)
		}
	}

	override fun compile(constructor: LlvmConstructor) {
		if(isVariadic)
			return
		val function = constructor.getParentFunction()
		llvmLocation = constructor.buildStackAllocation(type?.getLlvmType(constructor), name)
		val value = constructor.getParameter(function, index)
		constructor.buildStore(value, llvmLocation)
	}
}
