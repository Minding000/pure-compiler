package components.semantic_model.declarations

import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.code_generation.llvm.models.declarations.TypeAlias as TypeAliasUnit
import components.syntax_parser.syntax_tree.definitions.TypeAlias as TypeAliasSyntaxTree

// Consideration:
// Should TypeAliases mask the instances of the aliased type?
// Should the behaviour be toggleable using a keyword flag e.g. "masking alias ExitCode = Int"
class TypeAlias(override val source: TypeAliasSyntaxTree, scope: TypeScope, name: String, val referenceType: Type,
				val instances: List<Instance>):
	TypeDeclaration(source, name, scope, null, null, instances.toMutableList()) {
	override val isDefinition = false
	private var hasDeterminedEffectiveType = false
	private var effectiveType = referenceType.effectiveType
	val finalEffectiveType get() = effectiveType

	init {
		scope.typeDeclaration = this
		addSemanticModels(referenceType)
	}

	override fun getValueDeclaration(): ValueDeclaration {
		val targetScope = parentTypeDeclaration?.scope ?: scope.enclosingScope
		val staticType = StaticType(this)
		staticValueDeclaration = if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, staticType, null, true)
		else
			GlobalValueDeclaration(source, targetScope, name, staticType)
		return staticValueDeclaration
	}

	fun getEffectiveType(): Type {
		if(!context.declarationStack.push(this))
			return effectiveType
		if(!hasDeterminedEffectiveType) {
			hasDeterminedEffectiveType = true
			referenceType.determineTypes()
			if(referenceType is ObjectType) {
				val referenceTypeDeclaration = referenceType.getTypeDeclaration()
				if(referenceTypeDeclaration is TypeAlias)
					effectiveType = referenceTypeDeclaration.effectiveType
			}
		}
		context.declarationStack.pop(this)
		return effectiveType
	}

	override fun declare() {
		super.declare()
		scope.enclosingScope.addTypeDeclaration(this)
	}

	override fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		return referenceType.getConversionsFrom(sourceType)
	}

	override fun toUnit() = TypeAliasUnit(this, instances.map(Instance::toUnit))
}
