package components.linting.semantic_model.values

import errors.user.SignatureResolutionAmbiguityError
import components.linting.Linter
import components.linting.semantic_model.definitions.TypeDefinition
import components.linting.semantic_model.scopes.Scope
import components.linting.semantic_model.types.ObjectType
import components.linting.semantic_model.types.StaticType
import messages.Message
import components.syntax_parser.syntax_tree.definitions.Instance as InstanceSyntaxTree

open class Instance(override val source: InstanceSyntaxTree, value: VariableValue, val valueParameters: List<Value>):
	VariableValueDeclaration(source, value.name, null, value) {
	lateinit var typeDefinition: TypeDefinition

	init {
		value.staticValue = value
		units.addAll(valueParameters)
	}

	fun setType(typeDefinition: TypeDefinition) {
		this.typeDefinition = typeDefinition
		val type = ObjectType(typeDefinition)
		units.add(type)
		this.type = type
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		val staticType = StaticType(typeDefinition)
		units.add(staticType)
		try {
			val initializer = staticType.scope.resolveInitializer(valueParameters)
			if(initializer == null)
				linter.addMessage(source, "Initializer '${getSignature()}' hasn't been declared yet.",
					Message.Type.ERROR)
		} catch(error: SignatureResolutionAmbiguityError) {
			linter.addMessage(source, "Call to initializer '${getSignature()}' is ambiguous. " +
				"Matching signatures:" + error.signatures.joinToString("\n - ", "\n - "),
				Message.Type.ERROR) //TODO write test for this
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
