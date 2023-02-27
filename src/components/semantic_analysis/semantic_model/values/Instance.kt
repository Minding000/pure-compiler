package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import errors.user.SignatureResolutionAmbiguityError
import messages.Message
import components.syntax_parser.syntax_tree.definitions.Instance as InstanceSyntaxTree

class Instance(override val source: InstanceSyntaxTree, scope: Scope, override val value: VariableValue, val valueParameters: List<Value>):
	InterfaceMember(source, scope, value.name, null, value, true) {
	lateinit var typeDefinition: TypeDefinition

	init {
		addUnits(valueParameters)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Instance {
		return Instance(source, scope, value, valueParameters)
	}

	fun setType(typeDefinition: TypeDefinition) {
		this.typeDefinition = typeDefinition
		val type = ObjectType(typeDefinition)
		addUnits(type)
		this.type = type
	}

	override fun linkValues(linter: Linter) {
		value.definition = this
		value.type = type
		value.staticValue = value
		val staticType = StaticType(typeDefinition)
		addUnits(staticType)
		super.linkValues(linter)
		try {
			val initializer = staticType.interfaceScope.resolveInitializer(valueParameters)
			if(initializer == null)
				linter.addMessage(source, "Initializer '${getSignature()}' hasn't been declared yet.", Message.Type.ERROR)
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
