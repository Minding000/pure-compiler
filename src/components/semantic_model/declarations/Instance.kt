package components.semantic_model.declarations

import components.code_generation.llvm.models.declarations.Instance
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.SelfType
import components.semantic_model.types.StaticType
import components.semantic_model.values.Value
import errors.internal.CompilerError
import errors.user.SignatureResolutionAmbiguityError
import logger.issues.resolution.NotFound
import components.syntax_parser.syntax_tree.definitions.Instance as InstanceSyntaxTree

//TODO disallow instances in bound classes
class Instance(override val source: InstanceSyntaxTree, scope: MutableScope, name: String, val valueParameters: List<Value>,
			   isAbstract: Boolean, isOverriding: Boolean, val isNative: Boolean):
	InterfaceMember(source, scope, name, null, null, true, isAbstract, true, false, isOverriding) {
	lateinit var typeDeclaration: TypeDeclaration
	var initializer: InitializerDefinition? = null
	var conversions: Map<Value, InitializerDefinition>? = null

	init {
		addSemanticModels(valueParameters)
	}

	override fun determineType() {
		typeDeclaration = scope.getSurroundingTypeDeclaration()
			?: throw CompilerError(source, "Instance outside of type definition.")
		run {
			val typeDeclaration = typeDeclaration
			if(typeDeclaration is TypeAlias) {
				//TODO this should be an issue instead
				val effectiveObjectType = typeDeclaration.getEffectiveType() as? ObjectType
					?: throw CompilerError(source, "Instances are not allowed in inconcrete type aliases.")
				this.typeDeclaration = effectiveObjectType.getTypeDeclaration() ?: return
			}
		}
		val type = SelfType(typeDeclaration)
		addSemanticModels(type)
		providedType = type
		val staticType = StaticType(typeDeclaration)
		addSemanticModels(staticType)
		super.determineType()
		if(isAbstract || isNative)
			return
		try {
			val match = staticType.getInitializer(valueParameters)
			if(match == null) {
				context.addIssue(NotFound(source, "Initializer", getSignature()))
				return
			}
			initializer = match.initializer
			conversions = match.conversions
		} catch(error: SignatureResolutionAmbiguityError) {
			//TODO write test for this
			error.log(source, "initializer", getSignature())
		}
	}

	private fun getSignature(): String {
		var signature = typeDeclaration.name
		signature += "("
		signature += valueParameters.joinToString { parameter -> parameter.providedType.toString() }
		signature += ")"
		return signature
	}

	override fun toUnit(): Instance {
		val unit = Instance(this, valueParameters.map(Value::toUnit))
		this.unit = unit
		return unit
	}
}
