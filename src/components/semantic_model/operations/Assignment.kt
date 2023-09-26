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
		val value = sourceExpression.getLlvmValue(constructor)
		for(target in targets) {
			when(target) {
				is VariableValue -> {
					if(target.declaration is ComputedPropertyDeclaration) {
						TODO("Assignments to computed properties are not implemented yet.")
					} else {
						val sourceType = sourceExpression.type
						if(target.type is OptionalType && sourceType?.isLlvmPrimitive() == true) {
							val box = constructor.buildHeapAllocation(sourceType.getLlvmType(constructor), "_optionalPrimitiveBox")
							constructor.buildStore(value, box)
							constructor.buildStore(box, target.getLlvmLocation(constructor))
						} else {
							constructor.buildStore(value, target.getLlvmLocation(constructor))
						}
					}
				}
				is MemberAccess -> {
					if((target.member as? VariableValue)?.declaration is ComputedPropertyDeclaration) {
						TODO("Assignments to computed properties are not implemented yet.")
					} else {
						val sourceType = sourceExpression.type
						if(target.type is OptionalType && sourceType?.isLlvmPrimitive() == true) {
							val box = constructor.buildHeapAllocation(sourceType.getLlvmType(constructor), "_optionalPrimitiveBox")
							constructor.buildStore(value, box)
							constructor.buildStore(box, target.getLlvmLocation(constructor))
						} else {
							constructor.buildStore(value, target.getLlvmLocation(constructor))
						}
					}
				}
				is IndexAccess -> compileAssignmentToIndexAccess(constructor, target, value)
				else -> throw CompilerError(source, "Target of type '${target.javaClass.simpleName}' is not assignable.")
			}
		}
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
			val id = context.memberIdentities.getId(signature.toString(false, Operator.Kind.BRACKETS_SET))
			constructor.buildFunctionCall(context.llvmFunctionAddressFunctionType, context.llvmFunctionAddressFunction,
				listOf(classDefinitionAddress, constructor.buildInt32(id)), "indexOperatorAddress")
		}
		val parameters = LinkedList<LlvmValue>()
		parameters.add(targetValue)
		for(index in indexAccess.indices)
			parameters.add(index.getLlvmValue(constructor))
		parameters.add(value)
		constructor.buildFunctionCall(signature.getLlvmType(constructor), indexOperatorAddress, parameters)
	}
}
