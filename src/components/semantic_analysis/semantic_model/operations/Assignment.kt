package components.semantic_analysis.semantic_model.operations

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.Context
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.context.VariableUsage
import components.semantic_analysis.semantic_model.definitions.ComputedPropertyDeclaration
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.values.*
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
					if(target.member !is VariableValue || target.member.definition?.isConstant == true)
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
					if(target.definition is ComputedPropertyDeclaration) {
						TODO("Assignments to computed properties are not implemented yet.")
					} else {
						constructor.buildStore(value, target.getLlvmLocation(constructor))
					}
				}
				is MemberAccess -> {
					if((target.member as? VariableValue)?.definition is ComputedPropertyDeclaration) {
						TODO("Assignments to computed properties are not implemented yet.")
					} else {
						constructor.buildStore(value, target.getLlvmLocation(constructor))
					}
				}
				is IndexAccess -> {
					val signature = target.targetSignature ?: throw CompilerError(source, "Missing index operator implementation.")
					val indexTarget = target.target
					val targetValue = indexTarget.getLlvmValue(constructor)
					val indexOperatorAddress = if(indexTarget is SuperReference) {
						val operator = target.target.type?.interfaceScope?.resolveValue(Operator.Kind.BRACKETS_SET.stringRepresentation)
						val implementation = (operator?.value as? Operator)?.getImplementationBySignature(signature)
							?: throw CompilerError(source, "Failed to determine address of super index operator.")
						implementation.llvmValue
					} else {
						val classDefinitionAddressLocation = constructor.buildGetPropertyPointer(signature.parentDefinition?.llvmType,
							targetValue, Context.CLASS_DEFINITION_PROPERTY_INDEX, "classDefinition")
						val classDefinitionAddress = constructor.buildLoad(constructor.createPointerType(context.classDefinitionStruct),
							classDefinitionAddressLocation, "classDefinitionAddress")
						val id = context.memberIdentities.getId(signature.toString(false,
							Operator.Kind.BRACKETS_SET))
						constructor.buildFunctionCall(context.llvmFunctionAddressFunctionType, context.llvmFunctionAddressFunction,
							listOf(classDefinitionAddress, constructor.buildInt32(id)), "indexOperatorAddress")
					}
					val parameters = LinkedList<LlvmValue>()
					parameters.add(targetValue)
					for(index in target.indices)
						parameters.add(index.getLlvmValue(constructor))
					parameters.add(value)
					constructor.buildFunctionCall(signature.getLlvmType(constructor), indexOperatorAddress, parameters)
				}
				else -> throw CompilerError(source, "Target of type '${target.javaClass.simpleName}' is not assignable.")
			}
		}
	}
}
