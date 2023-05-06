package components.semantic_analysis.semantic_model.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.definitions.*
import components.semantic_analysis.semantic_model.operations.MemberAccess
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.VariableValue
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.initialization.ReliesOnUninitializedProperties
import logger.issues.modifiers.AbstractClassInstantiation
import logger.issues.resolution.NotCallable
import logger.issues.resolution.NotFound
import logger.issues.resolution.SignatureMismatch
import java.util.*
import components.syntax_parser.syntax_tree.control_flow.FunctionCall as FunctionCallSyntaxTree

class FunctionCall(override val source: FunctionCallSyntaxTree, scope: Scope, val function: Value, val typeParameters: List<Type>,
				   val valueParameters: List<Value>): Value(source, scope) {
	private var targetImplementation: MemberDeclaration? = null

	init {
		staticValue = this
		addUnits(typeParameters, valueParameters)
		addUnits(function)
	}

	override fun determineTypes(linter: Linter) {
		super.determineTypes(linter)
		when(val targetType = function.type) {
			is StaticType -> resolveInitializerCall(linter, targetType)
			is FunctionType -> resolveFunctionCall(linter, targetType)
			null -> {}
			else -> linter.addIssue(NotCallable(function))
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		val targetImplementation = targetImplementation
		if(targetImplementation !is Callable)
			return
		val requiredButUninitializedProperties = LinkedList<PropertyDeclaration>()
		for(propertyRequiredToBeInitialized in targetImplementation.propertiesRequiredToBeInitialized) {
			val usage = tracker.add(VariableUsage.Kind.READ, propertyRequiredToBeInitialized, this)
			if(!usage.isPreviouslyInitialized())
				requiredButUninitializedProperties.add(propertyRequiredToBeInitialized)
		}
		for(propertyBeingInitialized in targetImplementation.propertiesBeingInitialized)
			tracker.add(VariableUsage.Kind.WRITE, propertyBeingInitialized, this)
		if(tracker.isInitializer && requiredButUninitializedProperties.isNotEmpty())
			tracker.linter.addIssue(ReliesOnUninitializedProperties(source, getSignature(), requiredButUninitializedProperties))
	}

	override fun getComputedType(tracker: VariableTracker): Type? {
		return type
	}

	private fun resolveInitializerCall(linter: Linter, targetType: StaticType) {
		(targetType.definition as? Class)?.let { `class` ->
			if(`class`.isAbstract)
				linter.addIssue(AbstractClassInstantiation(source, `class`))
		}
		val baseDefinition = targetType.getBaseDefinition()
		val genericDefinitionTypes = baseDefinition.scope.getGenericTypeDefinitions()
		val definitionTypeParameters = (function as? TypeSpecification)?.typeParameters ?: listOf()
		try {
			val match = targetType.resolveInitializer(linter, genericDefinitionTypes, definitionTypeParameters, typeParameters,
				valueParameters)
			if(match == null) {
				linter.addIssue(NotFound(source, "Initializer", getSignature()))
				return
			}
			val type = ObjectType(match.definitionTypeSubstitutions.map { typeSubstitution -> typeSubstitution.value }, baseDefinition)
			type.determineTypes(linter)
			addUnits(type)
			this.type = type
			targetImplementation = match.signature
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(linter, source, "initializer", getSignature())
		}
	}

	private fun resolveFunctionCall(linter: Linter, functionType: FunctionType) {
		try {
			val signature = functionType.resolveSignature(linter, typeParameters, valueParameters)
			if(signature == null) {
				linter.addIssue(SignatureMismatch(function, typeParameters, valueParameters))
				return
			}
			type = signature.returnType
			val variable = function as? VariableValue
			val property = variable?.definition as? PropertyDeclaration
			val function = property?.value as? Function
			targetImplementation = function?.getImplementationBySignature(signature)
		} catch(error: SignatureResolutionAmbiguityError) {
			error.log(linter, source, "function", getSignature())
		}
	}

	private fun getSignature(): String {
		var signature = ""
		val function = function
		signature += when(function) {
			is VariableValue -> function.name
			is TypeSpecification -> function
			is MemberAccess -> "${function.target.type}.${function.member}"
			else -> "<anonymous function>"
		}
		signature += "("
		if(typeParameters.isNotEmpty()) {
			signature += typeParameters.joinToString()
			signature += ";"
			if(valueParameters.isNotEmpty())
				signature += " "
		}
		signature += valueParameters.joinToString { parameter -> parameter.type.toString() }
		signature += ")"
		return signature
	}
}
