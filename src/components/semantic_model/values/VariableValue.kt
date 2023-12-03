package components.semantic_model.values

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.declarations.PropertyDeclaration
import components.semantic_model.scopes.InterfaceScope
import components.semantic_model.scopes.Scope
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import errors.internal.CompilerError
import logger.issues.access.InstanceAccessFromStaticContext
import logger.issues.access.StaticAccessFromInstanceContext
import logger.issues.access.WhereClauseUnfulfilled
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
		val targetType = (scope as? InterfaceScope)?.type
		if(targetType != null) {
			if(valueDeclaration is ComputedPropertyDeclaration) {
				//TODO fix wrong condition (see FunctionCall)
				val whereClauseCondition = valueDeclaration.whereClauseConditions.firstOrNull()
				if(whereClauseCondition != null) {
					if(!whereClauseCondition.override.accepts(targetType))
						context.addIssue(WhereClauseUnfulfilled(source, "Computed property",
							"${valueDeclaration.parentTypeDeclaration.name}.${valueDeclaration.name}", targetType,
							whereClauseCondition))
				}
			}
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
		setUnextendedType(type)
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
		if(declaration is ComputedPropertyDeclaration)
			throw CompilerError(source, "Computed properties do not have a location.")
		val declaration = declaration
		return if(declaration is PropertyDeclaration) {
			context.resolveMember(constructor, declaration.parentTypeDeclaration.llvmType, context.getThisParameter(constructor), name,
				(declaration as? InterfaceMember)?.isStatic ?: false)
		} else {
			declaration?.llvmLocation
		}
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val declaration = declaration
		if(declaration is ComputedPropertyDeclaration) {
			val setStatement = declaration.setter
			if(setStatement != null && isIn(setStatement))
				return constructor.getLastParameter()
			return buildGetterCall(constructor, declaration)
		}
		return constructor.buildLoad(type?.getLlvmType(constructor), getLlvmLocation(constructor), name)
	}

	private fun buildGetterCall(constructor: LlvmConstructor, computedPropertyDeclaration: ComputedPropertyDeclaration): LlvmValue {
		val targetValue = context.getThisParameter(constructor)
		val functionAddress = context.resolveFunction(constructor, computedPropertyDeclaration.parentTypeDeclaration.llvmType, targetValue,
			computedPropertyDeclaration.getterIdentifier)
		val exceptionAddressLocation = constructor.buildStackAllocation(constructor.pointerType, "exceptionAddress")
		return constructor.buildFunctionCall(computedPropertyDeclaration.llvmGetterType, functionAddress,
			listOf(exceptionAddressLocation, targetValue), "_computedPropertyGetterResult")
		//TODO if exception exists
		// check for optional try (normal and force try have no effect)
		// check for catch
		// resume raise
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
