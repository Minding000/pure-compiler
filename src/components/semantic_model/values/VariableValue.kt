package components.semantic_model.values

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.declarations.PropertyDeclaration
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.declarations.WhereClauseCondition
import components.semantic_model.scopes.InterfaceScope
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
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
	var whereClauseConditions: List<WhereClauseCondition>? = null
	protected open var staticType: Type? = null

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

	override fun getLlvmLocation(constructor: LlvmConstructor): LlvmValue? {
		if(declaration is ComputedPropertyDeclaration)
			throw CompilerError(source, "Computed properties do not have a location.")
		val declaration = declaration
		return if(declaration is PropertyDeclaration) {
			//TODO same for other bound member accesses (e.g. member access with self target in bound class)
			var currentValue = context.getThisParameter(constructor)
			var currentTypeDeclaration = scope.getSurroundingTypeDeclaration() ?: return null
			while(!isDeclaredIn(declaration, currentTypeDeclaration)) {
				if(!currentTypeDeclaration.isBound)
					return null
				val parentProperty = constructor.buildGetPropertyPointer(currentTypeDeclaration.llvmType, currentValue,
					Context.PARENT_PROPERTY_INDEX, "_parentProperty")
				currentValue = constructor.buildLoad(constructor.pointerType, parentProperty, "_parent")
				currentTypeDeclaration = currentTypeDeclaration.parent?.scope?.getSurroundingTypeDeclaration() ?: return null
			}
			context.resolveMember(constructor, declaration.parentTypeDeclaration.llvmType, currentValue, name,
				(declaration as? InterfaceMember)?.isStatic ?: false)
		} else {
			declaration?.llvmLocation
		}
	}

	private fun isDeclaredIn(property: PropertyDeclaration, typeDeclaration: TypeDeclaration): Boolean {
		if(property.parentTypeDeclaration == typeDeclaration)
			return true
		for(superType in typeDeclaration.getDirectSuperTypes()) {
			if(isDeclaredIn(property, superType.getTypeDeclaration() ?: continue))
				return true
		}
		return false
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
