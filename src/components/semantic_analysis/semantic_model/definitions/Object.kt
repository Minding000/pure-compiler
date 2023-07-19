package components.semantic_analysis.semantic_model.definitions

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmType
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.context.Context
import components.semantic_analysis.semantic_model.context.IdentityMap
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
	lateinit var llvmClassDefinitionAddress: LlvmValue
	lateinit var llvmClassInitializer: LlvmValue
	lateinit var llvmClassInitializerType: LlvmType

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
		llvmClassInitializerType = constructor.buildFunctionType()
		llvmClassInitializer = constructor.buildFunction("${name}_ClassInitializer", llvmClassInitializerType)
		llvmType = constructor.declareStruct("${name}_ClassStruct")
		super.declare(constructor)
	}

	override fun define(constructor: LlvmConstructor) {
		defineLlvmStruct(constructor)
		defineLlvmClassInitializer(constructor)
		super.define(constructor)
	}

	private fun defineLlvmStruct(constructor: LlvmConstructor) {
		val members = LinkedList<LlvmType?>()
		members.add(constructor.createPointerType(context.classDefinitionStruct))
		for(memberDeclaration in scope.memberDeclarations) {
			if(memberDeclaration is ValueDeclaration) {
				members.add(memberDeclaration.type?.getLlvmType(constructor))
			}
		}
		constructor.defineStruct(llvmType, members)
	}

	private fun defineLlvmClassInitializer(constructor: LlvmConstructor) {
		constructor.createAndSelectBlock(llvmClassInitializer, "entrypoint")
		for(typeDefinition in scope.typeDefinitions.values) {
			if(typeDefinition is Object) {
				constructor.buildFunctionCall(typeDefinition.llvmClassInitializerType, typeDefinition.llvmClassInitializer)
			}
		}
		//TODO include super members in numbering
		// - map from class to index in each member (context.memberIdentifierIds)
		val memberCount = constructor.buildInt32(scope.memberDeclarations.size)
		val memberIdArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, memberCount, "memberIdArray")
		val memberOffsetArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, memberCount, "memberOffsetArray")
		for((memberIndex, memberDeclaration) in scope.memberDeclarations.withIndex()) {
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idLocation = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, memberIdArrayAddress, memberIndexValue, "memberIdLocation")
			if(memberDeclaration is ValueDeclaration) {
				val memberId = context.memberIdentities.register(memberDeclaration.memberIdentifier)
				val structMemberIndex = memberIndex + 1
				val memberOffset = constructor.getMemberOffsetInBytes(llvmType, structMemberIndex)
				println("'${memberDeclaration.memberIdentifier}' member offset: $memberOffset")
				val offsetLocation = constructor.buildGetArrayElementPointer(context.llvmMemberOffsetType, memberOffsetArrayAddress, memberIndexValue, "memberOffsetLocation")
				val memberIdValue = constructor.buildInt32(memberId)
				val memberOffsetValue = constructor.buildInt32(memberOffset)
				constructor.buildStore(memberIdValue, idLocation)
				constructor.buildStore(memberOffsetValue, offsetLocation)
			} else {
				val memberIdValue = constructor.buildInt32(IdentityMap.NULL_ID)
				constructor.buildStore(memberIdValue, idLocation)
			}
		}
		val initialStaticValues = LinkedList<LlvmValue>()
		initialStaticValues.add(memberCount)
		initialStaticValues.add(constructor.createNullPointer(constructor.voidType))
		initialStaticValues.add(constructor.createNullPointer(constructor.voidType))
		llvmClassDefinitionAddress = constructor.buildGlobal("${name}_ClassDefinition", context.classDefinitionStruct, constructor.buildConstantStruct(context.classDefinitionStruct, initialStaticValues))
		val memberIdArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.MEMBER_ID_ARRAY_PROPERTY_INDEX, "memberIdArray")
		val memberOffsetArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.MEMBER_OFFSET_ARRAY_PROPERTY_INDEX, "memberOffsetArray")
		constructor.buildStore(memberIdArrayAddress, memberIdArrayAddressLocation)
		constructor.buildStore(memberOffsetArrayAddress, memberOffsetArrayAddressLocation)
		constructor.buildReturn()
	}
}
