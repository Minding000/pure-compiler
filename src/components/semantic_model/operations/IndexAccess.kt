package components.semantic_model.operations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.code_generation.llvm.ValueConverter
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.FunctionSignature
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.Type
import components.semantic_model.values.Operator
import components.semantic_model.values.Value
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.access.AbstractMonomorphicAccess
import logger.issues.access.WhereClauseUnfulfilled
import logger.issues.resolution.NotFound
import java.util.*
import components.syntax_parser.syntax_tree.access.IndexAccess as IndexAccessSyntaxTree

class IndexAccess(override val source: IndexAccessSyntaxTree, scope: Scope, val target: Value, val typeParameters: List<Type>,
				  val indices: List<Value>): Value(source, scope) {
	var sourceExpression: Value? = null
	var targetSignature: FunctionSignature? = null
	override val hasGenericType: Boolean
		get() = targetSignature?.original?.returnType != targetSignature?.returnType

	init {
		addSemanticModels(typeParameters, indices)
		addSemanticModels(target)
	}

	override fun determineTypes() {
		determineSourceExpression()
		super.determineTypes()
		val targetType = target.providedType ?: return
		try {
			val match = targetType.interfaceScope.getIndexOperator(typeParameters, indices, sourceExpression)
			if(match == null) {
				val name = "${target.providedType}[${indices.joinToString { index -> index.providedType.toString() }}]"
				context.addIssue(NotFound(source, "Operator", "$name(${sourceExpression?.providedType ?: ""})"))
				return
			}
			targetSignature = match.signature
			setUnextendedType(match.returnType.getLocalType(this, targetType))
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(source, "operator", getSignature(targetType))
		}
	}

	private fun determineSourceExpression() {
		val parent = parent
		if(parent is Assignment && parent.targets.contains(this))
			sourceExpression = parent.sourceExpression
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		staticValue = this
	}

	override fun validate() {
		super.validate()
		validateWhereClauseConditions()
		validateMonomorphicAccess()
	}

	private fun validateWhereClauseConditions() {
		val signature = targetSignature ?: return
		val targetType = target.providedType ?: return
		val typeParameters = (targetType as? ObjectType)?.typeParameters ?: emptyList()
		for(condition in signature.whereClauseConditions) {
			if(!condition.isMet(typeParameters))
				context.addIssue(WhereClauseUnfulfilled(source, "Operator",
					signature.original.toString(false, getOperatorKind()), targetType, condition))
		}
	}

	private fun validateMonomorphicAccess() {
		val signature = targetSignature ?: return
		val targetType = target.providedType ?: return
		if(signature.associatedImplementation?.isAbstract == true && signature.associatedImplementation.isMonomorphic
			&& !targetType.isMemberAccessible(signature, true))
			context.addIssue(AbstractMonomorphicAccess(source, "operator",
				signature.toString(false, getOperatorKind()), targetType))
	}

	fun filterForPossibleTargetTypes(availableTypes: List<ObjectType>): List<ObjectType> {
		return availableTypes.filter { availableType ->
			availableType.interfaceScope.getIndexOperator(typeParameters, indices, sourceExpression) != null
		}
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val signature = targetSignature?.original ?: throw CompilerError(source, "Index access is missing a target.")
		return createLlvmFunctionCall(constructor, signature)
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature): LlvmValue {
		val targetValue = target.getLlvmValue(constructor) //TODO convert (write test)
		val parameters = LinkedList<LlvmValue>()
		parameters.add(context.getExceptionParameter(constructor))
		parameters.add(targetValue)
		for((indexIndex, index) in indices.withIndex()) {
			val parameterType = signature.getParameterTypeAt(indexIndex)
			parameters.add(ValueConverter.convertIfRequired(this, constructor, index.getLlvmValue(constructor), index.effectiveType,
				index.hasGenericType, parameterType, parameterType != signature.original.getParameterTypeAt(indexIndex)))
		}
		val sourceExpression = sourceExpression
		if(sourceExpression != null)
			parameters.add(sourceExpression.getLlvmValue(constructor)) //TODO convert (write test)
		val functionAddress = context.resolveFunction(constructor, targetValue,
			signature.original.toString(false, getOperatorKind()))
		val returnValue = constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters,
			"_indexAccess_result")
		context.continueRaise(constructor)
		return returnValue
	}

	private fun getSignature(targetType: Type, includeParentType: Boolean = true): String {
		var signature = ""
		if(includeParentType)
			signature += targetType.toString()
		signature += "["
		if(typeParameters.isNotEmpty()) {
			signature += typeParameters.joinToString()
			signature += ";"
			if(indices.isNotEmpty())
				signature += " "
		}
		signature += indices.joinToString { index -> index.providedType.toString() }
		signature += "]"
		sourceExpression?.let { sourceExpression ->
			signature += "(${sourceExpression.providedType})"
		}
		return signature
	}

	private fun getOperatorKind(): Operator.Kind {
		return if(sourceExpression == null) Operator.Kind.BRACKETS_GET else Operator.Kind.BRACKETS_SET
	}
}
