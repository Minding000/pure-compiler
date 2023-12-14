package components.semantic_model.values

import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.SelfType
import components.semantic_model.types.StaticType
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.definitions.Instance as InstanceSyntaxTree

//TODO disallow instances in bound classes
class Instance(override val source: InstanceSyntaxTree, scope: MutableScope, name: String, val valueParameters: List<Value>,
			   isAbstract: Boolean): InterfaceMember(source, scope, name, null, null, true, isAbstract) {
	lateinit var typeDeclaration: TypeDeclaration

	init {
		addSemanticModels(valueParameters)
	}

	override fun determineType() {
		this.typeDeclaration = scope.getSurroundingTypeDeclaration()
			?: throw CompilerError(source, "Instance outside of type definition.")
		val type = SelfType(typeDeclaration)
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
