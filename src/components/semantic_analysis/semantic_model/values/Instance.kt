package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
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
		addUnits(valueParameters)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): Instance {
		return Instance(source, scope, value, valueParameters, true)
	}

	override fun determineType(linter: Linter) {
		this.typeDefinition = scope.getSurroundingDefinition()
			?: throw CompilerError(source, "Instance outside of type definition.")
		val type = ObjectType(typeDefinition)
		addUnits(type)
		this.type = type
		value.definition = this
		value.type = type
		value.staticValue = value
		val staticType = StaticType(typeDefinition)
		addUnits(staticType)
		super.determineType(linter)
		try {
			val initializer = staticType.resolveInitializer(linter, valueParameters)
			if(initializer == null)
				linter.addIssue(NotFound(source, "Initializer", getSignature()))
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(linter, source, "initializer", getSignature())
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
