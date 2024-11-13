package components.semantic_model.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.operations.Assignment
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
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
	var conversions = HashMap<Value, InitializerDefinition>()

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
			val targetType = target.providedType
			if(sourceExpression.isAssignableTo(targetType)) {
				sourceExpression.setInferredType(targetType)
				continue
			}
			val sourceType = sourceExpression.providedType ?: continue
			if(targetType == null) {
				target.providedType = sourceType
				continue
			}
			val conversions = targetType.getConversionsFrom(sourceType)
			if(conversions.isNotEmpty()) {
				if(conversions.size > 1) {
					context.addIssue(ConversionAmbiguity(source, sourceType, targetType, conversions))
					continue
				}
				this.conversions[target] = conversions.first()
				continue
			}
			context.addIssue(TypeNotAssignable(source, sourceType, targetType))
		}
		registerSelfTypeUsages()
	}

	private fun registerSelfTypeUsages() {
		val sourceType = sourceExpression.providedType
		val baseSourceType = if(sourceType is OptionalType) sourceType.baseType else sourceType
		if(baseSourceType !is SelfType) {
			val surroundingFunction = scope.getSurroundingFunction()
			for(target in targets) {
				val targetType = target.providedType
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
					tracker.add(VariableUsage.Kind.WRITE, target, sourceExpression.providedType, sourceExpression.getComputedValue())
					continue
				}
				is MemberAccess -> {
					if(target.target is SelfReference && target.member is VariableValue) {
						tracker.add(VariableUsage.Kind.WRITE, target.member, sourceExpression.providedType,
							sourceExpression.getComputedValue())
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

	override fun toUnit() = Assignment(this, targets.map(Value::toUnit), sourceExpression.toUnit())

	override fun compile(constructor: LlvmConstructor) {
		val rawLlvmValue = sourceExpression.getLlvmValue(constructor)
		for(target in targets) {
			val conversion = conversions[target]
			//TODO optimization: cache converted value to avoid re-wrapping or re-boxing
			// cache key: targetType & isTargetGeneric & conversion
			when(target) {
				is VariableValue -> {
					val declaration = target.declaration
					val llvmValue = ValueConverter.convertIfRequired(this, constructor, rawLlvmValue, sourceExpression.effectiveType,
						sourceExpression.hasGenericType, declaration?.effectiveType, false, conversion)
					if(declaration is ComputedPropertyDeclaration)
						buildSetterCall(constructor, declaration, context.getThisParameter(constructor), llvmValue)
					else
						constructor.buildStore(llvmValue, target.getLlvmLocation(constructor))
				}
				is MemberAccess -> {
					val memberAccess = target
					val declaration = (memberAccess.member as? VariableValue)?.declaration
					val memberAccessTargetValue = memberAccess.target.getLlvmValue(constructor)
					val llvmValue = ValueConverter.convertIfRequired(this, constructor, rawLlvmValue, sourceExpression.effectiveType,
						sourceExpression.hasGenericType, memberAccess.member.effectiveType,
						memberAccess.member.effectiveType != declaration?.effectiveType, conversion)
					if(memberAccess.isOptional) {
						val function = constructor.getParentFunction()
						val writeBlock = constructor.createBlock(function, "optionalMemberAccess_write")
						val endBlock = constructor.createBlock(function, "optionalMemberAccess_end")
						constructor.buildJump(constructor.buildIsNull(memberAccessTargetValue, "_isTargetNull"), endBlock, writeBlock)
						constructor.select(writeBlock)
						//TODO fix: member access target value is calculated twice if not computed property (write test!)
						if(declaration is ComputedPropertyDeclaration)
							buildSetterCall(constructor, declaration, memberAccessTargetValue, llvmValue)
						else
							constructor.buildStore(llvmValue, memberAccess.getLlvmLocation(constructor))
						constructor.buildJump(endBlock)
						constructor.select(endBlock)
					} else {
						if(declaration is ComputedPropertyDeclaration)
							buildSetterCall(constructor, declaration, memberAccessTargetValue, llvmValue)
						else
							constructor.buildStore(llvmValue, memberAccess.getLlvmLocation(constructor))
					}
				}
				is IndexAccess -> {
					val targetType = target.targetSignature?.parameterTypes?.lastOrNull()
					val llvmValue = ValueConverter.convertIfRequired(this, constructor, rawLlvmValue, sourceExpression.effectiveType,
						sourceExpression.hasGenericType, targetType,
						targetType != target.targetSignature?.original?.parameterTypes?.lastOrNull(), conversion)
					compileAssignmentToIndexAccess(constructor, target, llvmValue)
				}
				else -> throw CompilerError(source, "Target of type '${target.javaClass.simpleName}' is not assignable.")
			}
		}
	}

	private fun buildSetterCall(constructor: LlvmConstructor, declaration: ComputedPropertyDeclaration, targetValue: LlvmValue,
								sourceValue: LlvmValue) {
		val exceptionAddress = context.getExceptionParameter(constructor)
		val parameters = LinkedList<LlvmValue>()
		parameters.add(exceptionAddress)
		parameters.add(targetValue)
		parameters.add(sourceValue)
		val functionAddress = context.resolveFunction(constructor, targetValue, declaration.setterIdentifier)
		constructor.buildFunctionCall(declaration.llvmSetterType, functionAddress, parameters)
		context.continueRaise(constructor, this)
	}

	private fun compileAssignmentToIndexAccess(constructor: LlvmConstructor, indexAccess: IndexAccess, value: LlvmValue) {
		val signature = indexAccess.targetSignature?.original
			?: throw CompilerError(source, "Missing index operator implementation.")
		val indexTarget = indexAccess.target
		val targetValue = indexTarget.getLlvmValue(constructor) //TODO convert (write test)
		val indexOperatorAddress = if(indexTarget is SuperReference) {
			val implementation = signature.associatedImplementation
				?: throw CompilerError(source, "Encountered member signature without implementation.")
			implementation.llvmValue
		} else {
			context.resolveFunction(constructor, targetValue, signature.getIdentifier(Operator.Kind.BRACKETS_SET))
		}
		val parameters = LinkedList<LlvmValue>()
		parameters.add(context.getExceptionParameter(constructor))
		parameters.add(targetValue)
		for((indexIndex, index) in indexAccess.indices.withIndex()) {
			val parameterType = signature.getParameterTypeAt(indexIndex)
			parameters.add(ValueConverter.convertIfRequired(this, constructor, index.getLlvmValue(constructor), index.effectiveType,
				index.hasGenericType, parameterType, parameterType != signature.original.getParameterTypeAt(indexIndex)))
		}
		parameters.add(value)
		constructor.buildFunctionCall(signature.getLlvmType(constructor), indexOperatorAddress, parameters)
		context.continueRaise(constructor, this)
	}
}
