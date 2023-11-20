package components.semantic_model.operations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
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
import logger.issues.resolution.NotFound
import java.util.*
import components.syntax_parser.syntax_tree.access.IndexAccess as IndexAccessSyntaxTree

class IndexAccess(override val source: IndexAccessSyntaxTree, scope: Scope, val target: Value, val typeParameters: List<Type>,
				  val indices: List<Value>): Value(source, scope) {
	var sourceExpression: Value? = null
	var targetSignature: FunctionSignature? = null

	init {
		addSemanticModels(typeParameters, indices)
		addSemanticModels(target)
	}

	override fun determineTypes() {
		determineSourceExpression()
		super.determineTypes()
		val targetType = target.type ?: return
		try {
			val match = targetType.interfaceScope.getIndexOperator(typeParameters, indices, sourceExpression)
			if(match == null) {
				val name = "${target.type}[${indices.joinToString { index -> index.type.toString() }}]"
				context.addIssue(NotFound(source, "Operator", "$name(${sourceExpression?.type ?: ""})"))
				return
			}
			targetSignature = match.signature
			type = match.returnType
			if(match.signature.associatedImplementation?.isAbstract == true && match.signature.associatedImplementation.isMonomorphic
				&& (targetType as? ObjectType)?.isSpecific == false)
				context.addIssue(AbstractMonomorphicAccess(source, "operator",
					match.signature.toString(false, getOperatorKind()), targetType))
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(source, "operator", getSignature(targetType))
		}
	}

	private fun determineSourceExpression() {
		val parent = parent
		if(parent is Assignment && parent.targets.contains(this))
			sourceExpression = parent.sourceExpression
	}

	private fun getSignature(targetType: Type): String {
		var signature = "$targetType["
		if(typeParameters.isNotEmpty()) {
			signature += typeParameters.joinToString()
			signature += ";"
			if(indices.isNotEmpty())
				signature += " "
		}
		signature += indices.joinToString { index -> index.type.toString() }
		signature += "]"
		sourceExpression?.let { sourceExpression ->
			signature += "(${sourceExpression.type})"
		}
		return signature
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		staticValue = this
	}

	fun filterForPossibleTargetTypes(availableTypes: List<ObjectType>): List<ObjectType> {
		return availableTypes.filter { availableType ->
			availableType.interfaceScope.getIndexOperator(typeParameters, indices, sourceExpression) != null
		}
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val signature = targetSignature?.original ?: throw CompilerError(source, "Index access is missing a target.")
		return createLlvmFunctionCall(constructor, signature)
	}

	private fun createLlvmFunctionCall(constructor: LlvmConstructor, signature: FunctionSignature): LlvmValue {
		val typeDefinition = signature.parentDefinition
		val targetValue = target.getLlvmValue(constructor)
		val exceptionAddressLocation = constructor.buildStackAllocation(constructor.pointerType, "exceptionAddress")
		val parameters = LinkedList<LlvmValue>()
		parameters.add(exceptionAddressLocation)
		parameters.add(targetValue)
		for(index in indices)
			parameters.add(index.getLlvmValue(constructor))
		val sourceExpression = sourceExpression
		if(sourceExpression != null)
			parameters.add(sourceExpression.getLlvmValue(constructor))
		val functionAddress = context.resolveFunction(constructor, typeDefinition?.llvmType, targetValue,
			signature.original.toString(false, getOperatorKind()))
		return constructor.buildFunctionCall(signature.getLlvmType(constructor), functionAddress, parameters, "_indexAccessResult")
		//TODO if exception exists
		// check for optional try (normal and force try have no effect)
		// check for catch
		// resume raise
	}

	private fun getOperatorKind(): Operator.Kind {
		return if(sourceExpression == null) Operator.Kind.BRACKETS_GET else Operator.Kind.BRACKETS_SET
	}
}
