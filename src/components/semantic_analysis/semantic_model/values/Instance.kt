package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.definitions.Instance as InstanceSyntaxTree

class Instance(override val source: InstanceSyntaxTree, scope: MutableScope, override val value: VariableValue,
			   val valueParameters: List<Value>, isSpecificCopy: Boolean = false):
	InterfaceMember(source, scope, value.name, null, value, true, isSpecificCopy = isSpecificCopy) {
	lateinit var typeDefinition: TypeDefinition

	init {
		addSemanticModels(valueParameters)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Instance {
		return Instance(source, scope, value, valueParameters, true)
	}

	override fun determineType() {
		this.typeDefinition = scope.getSurroundingDefinition()
			?: throw CompilerError(source, "Instance outside of type definition.")
		val type = ObjectType(typeDefinition)
		addSemanticModels(type)
		this.type = type
		value.definition = this
		value.type = type
		value.staticValue = value
		val staticType = StaticType(typeDefinition)
		addSemanticModels(staticType)
		super.determineType()
		try {
			val initializer = staticType.resolveInitializer(valueParameters)
			if(initializer == null)
				context.addIssue(NotFound(source, "Initializer", getSignature()))
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "initializer", getSignature())
		}
	}

	private fun getSignature(): String {
		var signature = typeDefinition.name
		signature += "("
		signature += valueParameters.joinToString { parameter -> parameter.type.toString() }
		signature += ")"
		return signature
	}
}
