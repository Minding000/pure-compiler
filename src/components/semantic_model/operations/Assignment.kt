package components.semantic_model.operations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.VariableTracker
import components.semantic_model.context.VariableUsage
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.OptionalType
import components.semantic_model.types.SelfType
import components.semantic_model.values.*
import errors.internal.CompilerError
import logger.issues.constant_conditions.ExpressionNotAssignable
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.initialization.ConstantReassignment
import logger.issues.resolution.ConversionAmbiguity
import java.util.*
import components.syntax_parser.syntax_tree.operations.Assignment as AssignmentSyntaxTree

class Assignment(override val source: AssignmentSyntaxTree, scope: Scope, val targets: List<Value>, val sourceExpression: Value):
	SemanticModel(source, scope) {
	private var conversion: InitializerDefinition? = null

	init {
		addSemanticModels(sourceExpression)
		addSemanticModels(targets)
	}

	override fun determineTypes() {
		super.determineTypes()
		for(target in targets) {
			if(target is IndexAccess)
				continue
			context.registerWrite(target)
			val targetType = target.type
			if(sourceExpression.isAssignableTo(targetType)) {
				sourceExpression.setInferredType(targetType)
				continue
			}
			val sourceType = sourceExpression.type ?: continue
			if(targetType == null) {
				target.type = sourceType
				continue
			}
			val conversions = targetType.getConversionsFrom(sourceType)
			if(conversions.isNotEmpty()) {
				if(conversions.size > 1) {
					context.addIssue(ConversionAmbiguity(source, sourceType, targetType, conversions))
					continue
				}
				conversion = conversions.first()
				continue
			}
			context.addIssue(TypeNotAssignable(source, sourceType, targetType))
		}
		registerSelfTypeUsages()
	}

	private fun registerSelfTypeUsages() {
		val sourceType = sourceExpression.type
		val baseSourceType = if(sourceType is OptionalType) sourceType.baseType else sourceType
		if(baseSourceType !is SelfType) {
			val surroundingFunction = scope.getSurroundingFunction()
			for(target in targets) {
				val targetType = target.type
				val baseTargetType = if(targetType is OptionalType) targetType.baseType else targetType
				if(baseTargetType is SelfType)
					surroundingFunction?.usesOwnTypeAsSelf = true
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		sourceExpression.analyseDataFlow(tracker)
		for(target in targets) {
			when(target) {
				is VariableValue -> {
					tracker.add(VariableUsage.Kind.WRITE, target, sourceExpression.type, sourceExpression.getComputedValue())
					continue
				}
				is MemberAccess -> {
					if(target.target is SelfReference && target.member is VariableValue) {
						tracker.add(VariableUsage.Kind.WRITE, target.member, sourceExpression.type, sourceExpression.getComputedValue())
						continue
					}
					if(target.member !is VariableValue || target.member.declaration?.isConstant == true)
						context.addIssue(ConstantReassignment(source, target.member.toString()))
				}
				is IndexAccess -> {}
				else -> context.addIssue(ExpressionNotAssignable(target.source))
			}
			target.analyseDataFlow(tracker)
		}
	}

	override fun compile(constructor: LlvmConstructor) {
		super.compile(constructor)
		val rawLlvmValue = sourceExpression.getLlvmValue(constructor)
		val sourceType = sourceExpression.type
		val pointerLlvmValue = if(sourceType?.isLlvmPrimitive() == true && targets.any { target -> target.type is OptionalType }) {
			val box = constructor.buildHeapAllocation(sourceType.getLlvmType(constructor), "_optionalPrimitiveBox")
			constructor.buildStore(rawLlvmValue, box)
			box
		} else {
			rawLlvmValue
		}
		for(target in targets) {
			val llvmValue = if(target.type is OptionalType) pointerLlvmValue else rawLlvmValue
			when(target) {
				is VariableValue -> {
					val declaration = target.declaration
					if(declaration is ComputedPropertyDeclaration)
						buildSetterCall(constructor, declaration, context.getThisParameter(constructor), llvmValue)
					else
						constructor.buildStore(llvmValue, target.getLlvmLocation(constructor))
				}
				is MemberAccess -> {
					val declaration = (target.member as? VariableValue)?.declaration
					if(declaration is ComputedPropertyDeclaration)
						buildSetterCall(constructor, declaration, target.target.getLlvmValue(constructor), llvmValue)
					else
						constructor.buildStore(llvmValue, target.getLlvmLocation(constructor))
				}
				is IndexAccess -> compileAssignmentToIndexAccess(constructor, target, llvmValue)
				else -> throw CompilerError(source, "Target of type '${target.javaClass.simpleName}' is not assignable.")
			}
		}
	}

	private fun buildSetterCall(constructor: LlvmConstructor, declaration: ComputedPropertyDeclaration, targetValue: LlvmValue,
								sourceValue: LlvmValue) {
		val parameters = LinkedList<LlvmValue>()
		val exceptionAddressLocation = constructor.buildStackAllocation(constructor.pointerType, "exceptionAddress")
		parameters.add(exceptionAddressLocation)
		parameters.add(targetValue)
		parameters.add(sourceValue)
		val functionAddress = context.resolveFunction(constructor, declaration.parentTypeDeclaration.llvmType, targetValue,
			declaration.setterIdentifier)
		constructor.buildFunctionCall(declaration.llvmSetterType, functionAddress, parameters)
		//TODO if exception exists
		// check for optional try (normal and force try have no effect)
		// check for catch
		// resume raise
	}

	private fun compileAssignmentToIndexAccess(constructor: LlvmConstructor, indexAccess: IndexAccess, value: LlvmValue) {
		val signature = indexAccess.targetSignature?.original
			?: throw CompilerError(source, "Missing index operator implementation.")
		val indexTarget = indexAccess.target
		val targetValue = indexTarget.getLlvmValue(constructor)
		val indexOperatorAddress = if(indexTarget is SuperReference) {
			val implementation = signature.associatedImplementation
				?: throw CompilerError(source, "Encountered member signature without implementation.")
			implementation.llvmValue
		} else {
			val classDefinitionAddressLocation = constructor.buildGetPropertyPointer(signature.parentDefinition?.llvmType,
				targetValue, Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinition")
			val classDefinitionAddress = constructor.buildLoad(constructor.pointerType, classDefinitionAddressLocation,
				"classDefinitionAddress")
			val id = context.memberIdentities.getId(signature.original.toString(false, Operator.Kind.BRACKETS_SET))
			constructor.buildFunctionCall(context.llvmFunctionAddressFunctionType, context.llvmFunctionAddressFunction,
				listOf(classDefinitionAddress, constructor.buildInt32(id)), "indexOperatorAddress")
		}
		val exceptionAddressLocation = constructor.buildStackAllocation(constructor.pointerType, "exceptionAddress")
		val parameters = LinkedList<LlvmValue>()
		parameters.add(exceptionAddressLocation)
		parameters.add(targetValue)
		for(index in indexAccess.indices)
			parameters.add(index.getLlvmValue(constructor))
		parameters.add(value)
		constructor.buildFunctionCall(signature.getLlvmType(constructor), indexOperatorAddress, parameters)
		//TODO if exception exists
		// check for optional try (normal and force try have no effect)
		// check for catch
		// resume raise
	}
}
