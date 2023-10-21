package components.semantic_model.declarations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.BlockScope
import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.AndUnionType
import components.semantic_model.types.ObjectType
import components.semantic_model.types.Type
import components.semantic_model.values.InterfaceMember
import components.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import logger.issues.declaration.CircularInheritance
import logger.issues.declaration.ExplicitParentOnScopedTypeDefinition
import logger.issues.declaration.MissingImplementations
import logger.issues.modifiers.NoParentToBindTo
import java.util.*

abstract class TypeDeclaration(override val source: SyntaxTreeNode, val name: String, override val scope: TypeScope,
							   val explicitParentType: ObjectType? = null, val superType: Type? = null,
							   val members: List<SemanticModel> = emptyList(), val isBound: Boolean = false): SemanticModel(source, scope) {
	open val isDefinition = true
	override var parent: SemanticModel?
		get() = super.parent
		set(value) {
			super.parent = value
			parentTypeDeclaration = value as? TypeDeclaration
		}
	var parentTypeDeclaration: TypeDeclaration? = null
	private var hasCircularInheritance = false
	var hasDeterminedTypes = false
	lateinit var staticValueDeclaration: ValueDeclaration
	private lateinit var staticMembers: List<ValueDeclaration>
	lateinit var properties: List<ValueDeclaration>
	private lateinit var functions: List<LlvmMemberFunction>
	lateinit var llvmType: LlvmType
	lateinit var llvmClassDefinitionAddress: LlvmValue
	lateinit var llvmClassInitializer: LlvmValue
	lateinit var llvmClassInitializerType: LlvmType
	lateinit var llvmStaticType: LlvmType

	init {
		addSemanticModels(explicitParentType, superType)
		addSemanticModels(members)
		for(member in members) {
			if(member is InterfaceMember)
				member.parentTypeDeclaration = this
		}
	}

	fun getLinkedSuperType(): Type? {
		superType?.determineTypes()
		return superType
	}

	fun getAllInitializers(): List<InitializerDefinition> {
		if(hasCircularInheritance)
			return emptyList()
		val allInitializers = LinkedList<InitializerDefinition>()
		allInitializers.addAll(scope.initializers)
		val superScope = scope.superScope ?: return allInitializers
		allInitializers.addAll(superScope.getAllInitializers())
		return allInitializers
	}

	open fun getValueDeclaration(): ValueDeclaration? = null

	override fun determineTypes() {
		if(hasDeterminedTypes)
			return
		hasDeterminedTypes = true
		explicitParentType?.determineTypes()
		if(explicitParentType != null) {
			if(parentTypeDeclaration == null) {
				parentTypeDeclaration = explicitParentType.getTypeDeclaration()
			} else {
				context.addIssue(ExplicitParentOnScopedTypeDefinition(explicitParentType.source))
			}
		}
		// Quick fix: Loading initializers first - bigger resolution rework required
		explicitParentType?.determineTypes()
		superType?.determineTypes()
		if(superType != null && inheritsFrom(this)) {
			hasCircularInheritance = true
			context.addIssue(CircularInheritance(superType.source))
		}
		for(semanticModel in semanticModels)
			if(semanticModel is InitializerDefinition)
				semanticModel.determineTypes()
		if(isDefinition && scope.initializers.isEmpty())
			addDefaultInitializer()
		for(semanticModel in semanticModels)
			if(semanticModel !is InitializerDefinition && semanticModel !== explicitParentType)
				semanticModel.determineTypes()
		scope.ensureUniqueInitializerSignatures()
		scope.inheritSignatures()
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		for(member in scope.memberDeclarations) {
			if(member is FunctionImplementation)
				member.analyseDataFlow(tracker)
		}
		if(!hasCircularInheritance) {
			for(initializer in scope.initializers)
				initializer.analyseDataFlow(tracker)
		}
	}

	override fun validate() {
		super.validate()
		if(isBound && parentTypeDeclaration == null)
			context.addIssue(NoParentToBindTo(source))
		if(isDefinition && (this as? Class)?.isAbstract != true && !hasCircularInheritance)
			ensureAbstractSuperMembersImplemented()
	}

	private fun ensureAbstractSuperMembersImplemented() {
		val missingOverrides = LinkedHashMap<TypeDeclaration, LinkedList<MemberDeclaration>>()
		for((unimplementedMember) in getUnimplementedAbstractSuperMemberDeclarations()) {
			val parentDefinition = unimplementedMember.parentTypeDeclaration
				?: throw CompilerError(unimplementedMember.source, "Member is missing parent definition.")
			val missingOverridesFromType = missingOverrides.getOrPut(parentDefinition) { LinkedList() }
			missingOverridesFromType.add(unimplementedMember)
		}
		if(missingOverrides.isEmpty())
			return
		context.addIssue(MissingImplementations(this, missingOverrides))
	}

	fun getUnimplementedAbstractSuperMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> {
		val abstractSuperMembers = superType?.getPotentiallyUnimplementedAbstractMemberDeclarations() ?: return emptyList()
		return abstractSuperMembers.filter { (abstractSuperMember, typeSubstitutions) ->
			!implements(abstractSuperMember, typeSubstitutions) }
	}

	fun implements(abstractMember: MemberDeclaration, typeSubstitutions: Map<TypeDeclaration, Type>): Boolean {
		return scope.memberDeclarations.any { memberDeclaration ->
			if(memberDeclaration == abstractMember)
				return false
			return@any if(memberDeclaration is PropertyDeclaration && abstractMember is PropertyDeclaration)
				memberDeclaration.name == abstractMember.name
			else if(memberDeclaration is FunctionImplementation && abstractMember is FunctionImplementation)
				memberDeclaration.signature.fulfillsInheritanceRequirementsOf(
					abstractMember.signature.withTypeSubstitutions(typeSubstitutions))
			else if(memberDeclaration is InitializerDefinition && abstractMember is InitializerDefinition)
				memberDeclaration.fulfillsInheritanceRequirementsOf(abstractMember, typeSubstitutions)
			else false
		} || superType?.implements(abstractMember, typeSubstitutions) ?: false
	}

	private fun addDefaultInitializer() {
		val defaultInitializer = InitializerDefinition(source, BlockScope(scope))
		defaultInitializer.determineTypes()
		addSemanticModels(defaultInitializer)
	}

	private fun inheritsFrom(definition: TypeDeclaration): Boolean {
		for(superType in getDirectSuperTypes()) {
			superType.determineTypes()
			val superTypeDeclaration = superType.getTypeDeclaration()
			if(superTypeDeclaration == definition)
				return true
			if(superTypeDeclaration?.inheritsFrom(definition) == true)
				return true
		}
		return false
	}

	fun getAllSuperTypes(): List<ObjectType> {
		val superTypes = LinkedList<ObjectType>()
		for(superType in getDirectSuperTypes()) {
			superTypes.add(superType)
			superTypes.addAll(superType.getTypeDeclaration()?.getAllSuperTypes() ?: continue)
		}
		return superTypes
	}

	fun getDirectSuperTypes(): List<ObjectType> {
		return when(val superType = superType) {
			is ObjectType -> listOf(superType)
			is AndUnionType -> superType.types.filterIsInstance<ObjectType>()
			else -> emptyList()
		}
	}

	open fun getConversionsFrom(sourceType: Type): List<InitializerDefinition> {
		determineTypes()
		return scope.getConversionsFrom(sourceType)
	}

	fun getGenericTypes(): List<ObjectType> {
		return scope.getGenericTypeDeclarations().map { genericTypeDeclaration -> ObjectType(genericTypeDeclaration) }
	}

	fun acceptsSubstituteType(substituteType: Type): Boolean { //TODO fix: it's not clear that Identifiable inherits from Any when running without STD lib
		if(superType == null)
			return false
		if(SpecialType.ANY.matches(superType))
			return true
		return superType.accepts(substituteType)
	}

	override fun equals(other: Any?): Boolean {
		if(other !is TypeDeclaration)
			return false
		return source == other.source
	}

	override fun hashCode(): Int {
		return source.hashCode()
	}

	override fun declare(constructor: LlvmConstructor) {
		if(isDefinition) {
			llvmClassInitializerType = constructor.buildFunctionType()
			llvmClassInitializer = constructor.buildFunction("${name}_ClassInitializer", llvmClassInitializerType)
			llvmStaticType = constructor.declareStruct("${name}_StaticStruct")
			llvmType = constructor.declareStruct("${name}_ClassStruct")
			llvmClassDefinitionAddress = constructor.declareGlobal("${name}_ClassDefinition", context.classDefinitionStruct)
			if(this !is Object)
				staticValueDeclaration.llvmLocation = constructor.declareGlobal("${name}_StaticObject", llvmStaticType)
		}
		super.declare(constructor)
	}

	override fun define(constructor: LlvmConstructor) {
		if(isDefinition) {
			staticMembers = getStaticMembers().values.toList()
			properties = getProperties().values.toList()
			functions = getFunctions().values.toList()
			defineLlvmStruct(constructor, staticMembers, properties)
			for(memberDeclaration in staticMembers)
				context.memberIdentities.register(memberDeclaration.name)
			for(property in properties)
				context.memberIdentities.register(property.name)
			for(function in functions)
				context.memberIdentities.register(function.identifier)
		}
		super.define(constructor)
	}

	override fun compile(constructor: LlvmConstructor) {
		if(isDefinition)
			buildLlvmClassInitializer(constructor, staticMembers, properties, functions)
		super.compile(constructor)
	}

	private fun getStaticMembers(): Map<String, ValueDeclaration> {
		val staticMembers = HashMap<String, ValueDeclaration>()
		for(member in scope.memberDeclarations)
			if((member as? InterfaceMember)?.isStatic == true)
				staticMembers[member.name] = member
		for(superType in getDirectSuperTypes()) {
			for(staticMember in superType.getTypeDeclaration()?.getStaticMembers()?.values ?: emptyList())
				staticMembers.putIfAbsent(staticMember.name, staticMember)
		}
		return staticMembers
	}

	private fun getProperties(): Map<String, ValueDeclaration> {
		val properties = HashMap<String, ValueDeclaration>()
		for(member in scope.memberDeclarations)
			if(member is ValueDeclaration && member !is ComputedPropertyDeclaration && (member as? InterfaceMember)?.isStatic != true)
				properties[member.name] = member
		for(superType in getDirectSuperTypes()) {
			for(property in superType.getTypeDeclaration()?.getProperties()?.values ?: emptyList())
				properties.putIfAbsent(property.name, property)
		}
		return properties
	}

	private fun getFunctions(): Map<String, LlvmMemberFunction> {
		val functions = HashMap<String, LlvmMemberFunction>()
		for(member in scope.memberDeclarations) {
//			if(member.isAbstract)
//				continue
			if(member is FunctionImplementation) {
				functions[member.memberIdentifier] = LlvmMemberFunction(member.memberIdentifier, member.llvmValue)
			} else if(member is ComputedPropertyDeclaration) {
				val llvmGetterValue = member.llvmGetterValue
				if(llvmGetterValue != null) {
					val getterIdentifier = member.getterIdentifier
					functions[getterIdentifier] = LlvmMemberFunction(getterIdentifier, llvmGetterValue)
				}
				val llvmSetterValue = member.llvmSetterValue
				if(llvmSetterValue != null) {
					val setterIdentifier = member.setterIdentifier
					functions[setterIdentifier] = LlvmMemberFunction(setterIdentifier, llvmSetterValue)
				}
			}
		}
		for(superType in getDirectSuperTypes()) {
			for(function in superType.getTypeDeclaration()?.getFunctions()?.values ?: emptyList())
				functions.putIfAbsent(function.identifier, function)
		}
		return functions
	}

	private fun defineLlvmStruct(constructor: LlvmConstructor, staticMembers: List<ValueDeclaration>, properties: List<ValueDeclaration>) {
		val llvmConstants = LinkedList<LlvmType?>()
		llvmConstants.add(constructor.pointerType)
		for(memberDeclaration in staticMembers)
			llvmConstants.add(memberDeclaration.type?.getLlvmType(constructor))
		constructor.defineStruct(llvmStaticType, llvmConstants)
		val llvmProperties = LinkedList<LlvmType?>()
		llvmProperties.add(constructor.pointerType)
		for(memberDeclaration in properties)
			llvmProperties.add(memberDeclaration.type?.getLlvmType(constructor))
		addNativeProperties(constructor, llvmProperties)
		constructor.defineStruct(llvmType, llvmProperties)
	}

	private fun addNativeProperties(constructor: LlvmConstructor, llvmProperties: LinkedList<LlvmType?>) {
		if(SpecialType.ARRAY.matches(this)) {
			context.arrayValueIndex = llvmProperties.size
			llvmProperties.add(constructor.pointerType)
		} else if(SpecialType.BYTE.matches(this)) {
			context.byteValueIndex = llvmProperties.size
			llvmProperties.add(constructor.byteType)
		} else if(SpecialType.INTEGER.matches(this)) {
			context.integerValueIndex = llvmProperties.size
			llvmProperties.add(constructor.i32Type)
		} else if(SpecialType.FLOAT.matches(this)) {
			context.floatValueIndex = llvmProperties.size
			llvmProperties.add(constructor.floatType)
		}
	}

	private fun buildLlvmClassInitializer(constructor: LlvmConstructor, staticMembers: List<ValueDeclaration>,
										  properties: List<ValueDeclaration>, functions: List<LlvmMemberFunction>) {
		println("'$name' class initializer:")
		val previousBlock = constructor.getCurrentBlock()
		constructor.createAndSelectBlock(llvmClassInitializer, "entrypoint")
		for(typeDeclaration in scope.typeDeclarations.values) {
			if(typeDeclaration.isDefinition)
				constructor.buildFunctionCall(typeDeclaration.llvmClassInitializerType, typeDeclaration.llvmClassInitializer)
		}
		val staticMemberCount = constructor.buildInt32(staticMembers.size)
		val staticMemberIdArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, staticMemberCount, "staticMemberIdArray")
		val staticMemberOffsetArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberOffsetType, staticMemberCount, "staticMemberOffsetArray")
		val propertyCount = constructor.buildInt32(properties.size)
		val propertyIdArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, propertyCount, "propertyIdArray")
		val propertyOffsetArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberOffsetType, propertyCount, "propertyOffsetArray")
		val functionCount = constructor.buildInt32(functions.size)
		val functionIdArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, functionCount, "functionIdArray")
		val functionAddressArrayAddress = constructor.buildHeapArrayAllocation(context.llvmMemberAddressType, functionCount, "functionAddressArray")
		for((memberIndex, memberDeclaration) in staticMembers.withIndex()) {
			val memberId = context.memberIdentities.getId(memberDeclaration.name)
			val structMemberIndex = memberIndex + 1
			val memberOffset = constructor.getMemberOffsetInBytes(llvmStaticType, structMemberIndex)
			println("Mapping static member '${memberDeclaration.name}' to ID '$memberId' with offset '$memberOffset'.")
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idLocation = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, staticMemberIdArrayAddress, memberIndexValue, "staticMemberIdLocation")
			val offsetLocation = constructor.buildGetArrayElementPointer(context.llvmMemberOffsetType, staticMemberOffsetArrayAddress, memberIndexValue, "staticMemberOffsetLocation")
			val memberIdValue = constructor.buildInt32(memberId)
			val memberOffsetValue = constructor.buildInt32(memberOffset)
			constructor.buildStore(memberIdValue, idLocation)
			constructor.buildStore(memberOffsetValue, offsetLocation)
		}
		for((memberIndex, property) in properties.withIndex()) {
			val memberId = context.memberIdentities.getId(property.name)
			val structMemberIndex = memberIndex + 1
			val memberOffset = constructor.getMemberOffsetInBytes(llvmType, structMemberIndex)
			println("Mapping property '${property.name}' to ID '$memberId' with offset '$memberOffset'.")
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idLocation = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, propertyIdArrayAddress, memberIndexValue, "propertyIdLocation")
			val offsetLocation = constructor.buildGetArrayElementPointer(context.llvmMemberOffsetType, propertyOffsetArrayAddress, memberIndexValue, "propertyOffsetLocation")
			val memberIdValue = constructor.buildInt32(memberId)
			val memberOffsetValue = constructor.buildInt32(memberOffset)
			constructor.buildStore(memberIdValue, idLocation)
			constructor.buildStore(memberOffsetValue, offsetLocation)
		}
		for((memberIndex, function) in functions.withIndex()) {
			val memberId = context.memberIdentities.getId(function.identifier)
			println("Mapping function '${function.identifier}' to ID '$memberId'.")
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idLocation = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, functionIdArrayAddress, memberIndexValue, "functionIdLocation")
			val addressLocation = constructor.buildGetArrayElementPointer(context.llvmMemberAddressType, functionAddressArrayAddress, memberIndexValue, "functionAddressLocation")
			val memberIdValue = constructor.buildInt32(memberId)
			constructor.buildStore(memberIdValue, idLocation)
			constructor.buildStore(function.llvmValue, addressLocation)
		}
		val initialStaticValues = LinkedList<LlvmValue>()
		initialStaticValues.add(staticMemberCount)
		initialStaticValues.add(constructor.nullPointer)
		initialStaticValues.add(constructor.nullPointer)
		initialStaticValues.add(propertyCount)
		initialStaticValues.add(constructor.nullPointer)
		initialStaticValues.add(constructor.nullPointer)
		initialStaticValues.add(functionCount)
		initialStaticValues.add(constructor.nullPointer)
		initialStaticValues.add(constructor.nullPointer)
		constructor.defineGlobal(llvmClassDefinitionAddress, constructor.buildConstantStruct(context.classDefinitionStruct, initialStaticValues))
		val staticMemberIdArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.CONSTANT_ID_ARRAY_PROPERTY_INDEX, "staticMemberIdArray")
		val staticMemberOffsetArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.CONSTANT_OFFSET_ARRAY_PROPERTY_INDEX, "staticMemberOffsetArray")
		val propertyIdArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.PROPERTY_ID_ARRAY_PROPERTY_INDEX, "propertyIdArray")
		val propertyOffsetArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.PROPERTY_OFFSET_ARRAY_PROPERTY_INDEX, "propertyOffsetArray")
		val functionIdArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.FUNCTION_ID_ARRAY_PROPERTY_INDEX, "functionIdArray")
		val functionAddressArrayAddressLocation = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinitionAddress, Context.FUNCTION_ADDRESS_ARRAY_PROPERTY_INDEX, "functionAddressArray")
		constructor.buildStore(staticMemberIdArrayAddress, staticMemberIdArrayAddressLocation)
		constructor.buildStore(staticMemberOffsetArrayAddress, staticMemberOffsetArrayAddressLocation)
		constructor.buildStore(propertyIdArrayAddress, propertyIdArrayAddressLocation)
		constructor.buildStore(propertyOffsetArrayAddress, propertyOffsetArrayAddressLocation)
		constructor.buildStore(functionIdArrayAddress, functionIdArrayAddressLocation)
		constructor.buildStore(functionAddressArrayAddress, functionAddressArrayAddressLocation)
		if(this !is Object) {
			val values = LinkedList<LlvmValue>()
			values.add(llvmClassDefinitionAddress)
			for(staticMember in staticMembers) {
				val staticMemberValue = staticMember.value?.getComputedValue()
				val staticMemberLlvmValue = if(staticMemberValue?.type?.isLlvmPrimitive() == true)
					staticMemberValue.getLlvmValue(constructor)
				else
					constructor.nullPointer
				values.add(staticMemberLlvmValue)
			}
			constructor.defineGlobal(staticValueDeclaration.llvmLocation, constructor.buildConstantStruct(llvmStaticType, values))
			//TODO initialize non-staticMember static members
		}
		constructor.buildReturn()
		constructor.select(previousBlock)
	}

	override fun toString(): String {
		if(superType == null || SpecialType.ANY.matches(superType))
			return name
		return "$name: $superType"
	}

	class LlvmMemberFunction(val identifier: String, val llvmValue: LlvmValue)
}
