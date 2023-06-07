package components.semantic_analysis.semantic_model.definitions

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import java.util.*
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Object(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, explicitParentType: ObjectType?,
			 superType: Type?, members: List<SemanticModel>, isBound: Boolean, val isNative: Boolean, val isMutable: Boolean):
	TypeDefinition(source, name, scope, explicitParentType, superType, members, isBound) {

	init {
		scope.typeDefinition = this
	}

	override fun getValueDeclaration(): ValueDeclaration {
		val targetScope = parentTypeDefinition?.scope ?: scope.enclosingScope
		val type = ObjectType(this)
		return if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, type, null, !isBound)
		else
			LocalVariableDeclaration(source, targetScope, name, type)
	}

	override fun declare() {
		super.declare()
		val targetScope = parentTypeDefinition?.scope ?: scope.enclosingScope
		targetScope.declareType(this)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Object {
		return this //TODO What about bound objects?
	}

	override fun validate() {
		super.validate()
		scope.ensureTrivialInitializers()
	}

	override fun declare(constructor: LlvmConstructor) {
		super.declare(constructor)
		llvmType = constructor.declareStruct(name)
	}

	override fun compile(constructor: LlvmConstructor) {
		super.compile(constructor)
		val members = LinkedList<LlvmType?>()
		for(memberDeclaration in scope.memberDeclarations) {
			if(memberDeclaration is ValueDeclaration) {
				members.add(memberDeclaration.type?.getLlvmReference(constructor))
			}
		}
		constructor.defineStruct(llvmType, members)
	}
}
