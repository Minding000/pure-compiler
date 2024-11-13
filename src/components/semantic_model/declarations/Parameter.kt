package components.semantic_model.declarations

import components.code_generation.llvm.models.declarations.Parameter
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.declaration.PropertyParameterMismatch
import logger.issues.declaration.PropertyParameterOutsideOfInitializer
import kotlin.properties.Delegates

class Parameter(override val source: SyntaxTreeNode, scope: MutableScope, name: String, type: Type?, isMutable: Boolean = false,
				val isVariadic: Boolean = false): ValueDeclaration(source, scope, name, type, null, true, isMutable) {
	val isPropertySetter = type == null
	var propertyDeclaration: ValueDeclaration? = null
	val hasGenericType: Boolean
		get() {
			val surroundingFunction = scope.getSurroundingFunction() ?: return false
			return effectiveType != surroundingFunction.signature.getEffectiveParameterType(surroundingFunction.parameters.indexOf(this))
		}
	var index by Delegates.notNull<Int>()

	override fun declare() {
		if(providedType != null)
			scope.addValueDeclaration(this)
	}

	override fun determineType() {
		if(isPropertySetter) {
			val parent = parent
			if(parent is InitializerDefinition) {
				//TODO disallow computed property with where clause as target
				//TODO allow/disallow property setter for inherited property (write tests!)
				val match = parent.parentTypeDeclaration.scope.getValueDeclaration(name)
				propertyDeclaration = match?.declaration
				providedType = match?.type
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
			val propertyDeclaration = propertyDeclaration
			if(propertyDeclaration != null)
				tracker.add(VariableUsage.Kind.WRITE, propertyDeclaration, this)
		} else {
			tracker.declare(this, true)
		}
	}

	override fun toUnit(): Parameter {
		val unit = Parameter(this)
		this.unit = unit
		return unit
	}

	override fun compile(constructor: LlvmConstructor) {
		if(isVariadic)
			return
		val function = constructor.getParentFunction()
		val value = constructor.getParameter(function, index)
		llvmLocation = constructor.buildStackAllocation(constructor.getParameterType(function, index), name)
		constructor.buildStore(value, llvmLocation)
	}
}
