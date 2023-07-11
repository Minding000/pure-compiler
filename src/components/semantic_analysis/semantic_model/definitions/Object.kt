package components.semantic_analysis.semantic_model.definitions

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import java.util.*
import components.syntax_parser.syntax_tree.definitions.TypeDefinition as TypeDefinitionSyntaxTree

class Object(override val source: TypeDefinitionSyntaxTree, name: String, scope: TypeScope, explicitParentType: ObjectType?,
			 superType: Type?, members: List<SemanticModel>, isBound: Boolean, val isNative: Boolean, val isMutable: Boolean):
	TypeDefinition(source, name, scope, explicitParentType, superType, members, isBound) {
	lateinit var classDefinitionLocation: LlvmValue

	init {
		scope.typeDefinition = this
	}

	override fun getValueDeclaration(): ValueDeclaration {
		val targetScope = parentTypeDefinition?.scope ?: scope.enclosingScope
		val staticType = StaticType(this)
		val value = FunctionCall(source, scope, Value(source, scope, staticType))
		val type = ObjectType(this)
		return if(targetScope is TypeScope)
			PropertyDeclaration(source, targetScope, name, type, value, !isBound)
		else
			LocalVariableDeclaration(source, targetScope, name, type, value)
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
		//TODO These malloc calls need to be in a function (so the global needs a placeholder value)
//		val memberCount = constructor.buildInt32(members.size)
//		val memberIdArrayLocation = constructor.buildArray(constructor.i32Type, memberCount, "memberIdArray")
//		val memberLocationArrayLocation = constructor.buildArray(constructor.i32Type, memberCount, "memberLocationArray")
//		val values = LinkedList<LlvmValue>()
//		values.add(memberCount)
//		values.add(memberIdArrayLocation)
//		values.add(memberLocationArrayLocation)
//		classDefinitionLocation = constructor.buildGlobal("${name}Definition", context.classStruct, constructor.buildConstantStruct(context.classStruct, values))

		//TODO include super members in numbering
		// - map from class to index in each member (context.memberIdentifierIds)
		llvmType = constructor.declareStruct("${name}Struct")
		var memberIndex = 1
		for(memberDeclaration in scope.memberDeclarations) {
			if(memberDeclaration is ValueDeclaration) {
				memberDeclaration.memberIndex = memberIndex
				memberIndex++
			}
		}
		super.declare(constructor)
	}

	override fun define(constructor: LlvmConstructor) {
		val members = LinkedList<LlvmType?>()
		members.add(constructor.createPointerType(context.classStruct))
		for(memberDeclaration in scope.memberDeclarations) {
			if(memberDeclaration is ValueDeclaration) {
				members.add(memberDeclaration.type?.getLlvmType(constructor))
			}
		}
		constructor.defineStruct(llvmType, members)
		super.define(constructor)
	}
}
