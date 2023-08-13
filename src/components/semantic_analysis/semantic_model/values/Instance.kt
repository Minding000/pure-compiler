package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.definitions.Instance as InstanceSyntaxTree

class Instance(override val source: InstanceSyntaxTree, scope: MutableScope, name: String, val valueParameters: List<Value>,
			   isSpecificCopy: Boolean = false):
	InterfaceMember(source, scope, name, null, null, true, isSpecificCopy = isSpecificCopy) {
	lateinit var typeDeclaration: TypeDeclaration

	init {
		addSemanticModels(valueParameters)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): Instance {
		return Instance(source, scope, name, valueParameters, true)
	}

	override fun determineType() {
		this.typeDeclaration = scope.getSurroundingTypeDeclaration()
			?: throw CompilerError(source, "Instance outside of type definition.")
		val type = ObjectType(typeDeclaration)
		addSemanticModels(type)
		this.type = type
		val staticType = StaticType(typeDeclaration)
		addSemanticModels(staticType)
		super.determineType()
		try {
			val initializer = staticType.getInitializer(valueParameters)
			if(initializer == null)
				context.addIssue(NotFound(source, "Initializer", getSignature()))
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "initializer", getSignature())
		}
	}

	private fun getSignature(): String {
		var signature = typeDeclaration.name
		signature += "("
		signature += valueParameters.joinToString { parameter -> parameter.type.toString() }
		signature += ")"
		return signature
	}
}
