package components.semantic_model.declarations

import components.code_generation.llvm.*
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.BlockScope
import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.AndUnionType
import components.semantic_model.types.ObjectType
import components.semantic_model.types.StaticType
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import errors.internal.CompilerError
import logger.issues.declaration.*
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
	private var hasDeterminedTypes = false
	lateinit var staticValueDeclaration: ValueDeclaration
	private lateinit var staticMembers: List<ValueDeclaration>
	lateinit var properties: List<ValueDeclaration>
	private lateinit var functions: List<LlvmMemberFunction>
	lateinit var llvmType: LlvmType
	lateinit var llvmClassDefinition: LlvmValue
	lateinit var llvmClassInitializer: LlvmValue
	lateinit var llvmClassInitializerType: LlvmType
	lateinit var llvmCommonPreInitializer: LlvmValue
	lateinit var llvmCommonPreInitializerType: LlvmType
	lateinit var llvmStaticType: LlvmType
	private var cachedLlvmMetadata: LlvmDebugInfoMetadata? = null

	companion object {
		const val FIXED_STATIC_PROPERTY_COUNT = 1
		const val FIXED_PROPERTY_COUNT = 1
	}

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
		for(semanticModel in semanticModels) {
			if(semanticModel !is InitializerDefinition)
				semanticModel.analyseDataFlow(tracker)
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
		if(isDefinition) {
			if((this as? Class)?.isAbstract != true && !hasCircularInheritance)
				ensureAbstractSuperMembersImplemented()
			ensureSpecificMembersOverridden()
			validateMonomorphicSuperMember()
		}
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
			!implements(abstractSuperMember, typeSubstitutions)
		}
	}

	fun implements(abstractMember: MemberDeclaration, typeSubstitutions: Map<TypeDeclaration, Type>): Boolean {
		return scope.memberDeclarations.any { memberDeclaration ->
			if(memberDeclaration == abstractMember)
				return false
			return@any if(memberDeclaration is PropertyDeclaration && abstractMember is PropertyDeclaration)
				memberDeclaration.name == abstractMember.name
			else if(memberDeclaration is FunctionImplementation && abstractMember is FunctionImplementation)
				memberDeclaration.fulfillsInheritanceRequirementsOf(abstractMember, typeSubstitutions)
			else if(memberDeclaration is InitializerDefinition && abstractMember is InitializerDefinition)
				memberDeclaration.fulfillsInheritanceRequirementsOf(abstractMember, typeSubstitutions)
			else if(memberDeclaration is Instance && abstractMember is Instance)
				memberDeclaration.name == abstractMember.name
			else false
		} || superType?.implements(abstractMember, typeSubstitutions) ?: false
	}

	private fun ensureSpecificMembersOverridden() {
		val missingOverrides = LinkedHashMap<TypeDeclaration, LinkedList<MemberDeclaration>>()
		for((unimplementedMember) in getUnoverriddenSpecificSuperMemberDeclarations()) {
			val parentDefinition = unimplementedMember.parentTypeDeclaration
				?: throw CompilerError(unimplementedMember.source, "Member is missing parent definition.")
			val missingOverridesFromType = missingOverrides.getOrPut(parentDefinition) { LinkedList() }
			missingOverridesFromType.add(unimplementedMember)
		}
		if(missingOverrides.isEmpty())
			return
		context.addIssue(MissingSpecificOverrides(this, missingOverrides))
	}

	private fun getUnoverriddenSpecificSuperMemberDeclarations(): List<Pair<MemberDeclaration, Map<TypeDeclaration, Type>>> {
		val specificSuperMembers = superType?.getSpecificMemberDeclarations() ?: return emptyList()
		return specificSuperMembers.filter { (specificSuperMember, typeSubstitutions) ->
			!overrides(specificSuperMember, typeSubstitutions)
		}
	}

	private fun overrides(specificMember: MemberDeclaration, typeSubstitutions: Map<TypeDeclaration, Type>): Boolean {
		return scope.memberDeclarations.any { memberDeclaration ->
			if(memberDeclaration == specificMember)
				return false
			if(memberDeclaration !is FunctionImplementation || specificMember !is FunctionImplementation)
				return@any false
			return@any memberDeclaration.signature.fulfillsInheritanceRequirementsOf(
				specificMember.signature.withTypeSubstitutions(typeSubstitutions))
		}
	}

	private fun validateMonomorphicSuperMember() {
		val superTypeContainsMonomorphicMemberImplementations = getDirectSuperTypes().any { superType ->
			val classDeclaration = superType.getTypeDeclaration() as? Class ?: return@any false
			return@any !classDeclaration.isAbstract && classDeclaration.containsMonomorphicMemberImplementation()
		}
		if(superTypeContainsMonomorphicMemberImplementations)
			context.addIssue(MonomorphicInheritance(source))
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

	fun acceptsSubstituteType(
		substituteType: Type): Boolean { //TODO fix: it's not clear that Identifiable inherits from Any when running without STD lib
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
			declareClassInitializer(constructor)
			declareCommonPreInitializer(constructor)
			llvmStaticType = constructor.declareStruct("${getFullName()}_StaticStruct")
			llvmType = constructor.declareStruct("${getFullName()}_ClassStruct")
			llvmClassDefinition = constructor.declareGlobal("${getFullName()}_ClassDefinition", context.classDefinitionStruct)
			if(this !is Object)
				staticValueDeclaration.llvmLocation = constructor.declareGlobal("${getFullName()}_StaticObject", llvmStaticType)
		}
		super.declare(constructor)
	}

	private fun declareClassInitializer(constructor: LlvmConstructor) {
		llvmClassInitializerType = constructor.buildFunctionType(listOf(constructor.pointerType))
		llvmClassInitializer = constructor.buildFunction("${getFullName()}_ClassInitializer", llvmClassInitializerType)
	}

	private fun declareCommonPreInitializer(constructor: LlvmConstructor) {
		val parameterTypes = LinkedList<LlvmType?>()
		parameterTypes.add(Context.EXCEPTION_PARAMETER_INDEX, constructor.pointerType)
		parameterTypes.add(Context.THIS_PARAMETER_INDEX, constructor.pointerType)
		var parameterIndex = Context.VALUE_PARAMETER_OFFSET
		if(isBound) {
			parameterTypes.add(Context.PARENT_PARAMETER_OFFSET, constructor.pointerType)
			parameterIndex++
		}
		for(genericTypeDeclaration in scope.getGenericTypeDeclarations()) {
			parameterTypes.add(constructor.pointerType)
			genericTypeDeclaration.index = parameterIndex
			parameterIndex++
		}
		llvmCommonPreInitializerType = constructor.buildFunctionType(parameterTypes)
		llvmCommonPreInitializer = constructor.buildFunction("${getFullName()}_CommonPreInitializer", llvmCommonPreInitializerType)
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
		if(isDefinition) {
			buildLlvmClassInitializer(constructor, staticMembers, properties, functions)
			buildLlvmCommonPreInitializer(constructor, properties)
		}
		super.compile(constructor)
	}

	private fun getStaticMembers(): Map<String, ValueDeclaration> {
		val staticMembers = HashMap<String, ValueDeclaration>()
		for(member in scope.memberDeclarations)
			if((member as? InterfaceMember)?.isStatic == true && (member.providedType as? StaticType)?.typeDeclaration !is TypeAlias)
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
			if(member is FunctionImplementation) {
				val memberIdentifier = member.memberIdentifier
				if(!member.isAbstract)
					functions[memberIdentifier] = LlvmMemberFunction(memberIdentifier, member.llvmValue)
			} else if(member is ComputedPropertyDeclaration) {
				val llvmGetterValue = member.llvmGetterValue
				if(llvmGetterValue != null) {
					val getterIdentifier = member.getterIdentifier
					if(!member.isAbstract)
						functions[getterIdentifier] = LlvmMemberFunction(getterIdentifier, llvmGetterValue)
				}
				val llvmSetterValue = member.llvmSetterValue
				if(llvmSetterValue != null) {
					val setterIdentifier = member.setterIdentifier
					if(!member.isAbstract)
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
			llvmConstants.add(memberDeclaration.effectiveType?.getLlvmType(constructor))
		constructor.defineStruct(llvmStaticType, llvmConstants)
		val llvmProperties = LinkedList<LlvmType?>()
		llvmProperties.add(constructor.pointerType)
		if(isBound)
			llvmProperties.add(constructor.pointerType)
		for(memberDeclaration in properties)
			llvmProperties.add(memberDeclaration.effectiveType?.getLlvmType(constructor))
		addNativeProperties(constructor, llvmProperties)
		constructor.defineStruct(llvmType, llvmProperties)
	}

	private fun addNativeProperties(constructor: LlvmConstructor, llvmProperties: LinkedList<LlvmType?>) {
		if(SpecialType.ARRAY.matches(this)) {
			context.arrayValueIndex = llvmProperties.size
			llvmProperties.add(constructor.pointerType)
		} else if(SpecialType.BOOLEAN.matches(this)) {
			context.booleanValueIndex = llvmProperties.size
			llvmProperties.add(constructor.booleanType)
		} else if(SpecialType.BYTE.matches(this)) {
			context.byteValueIndex = llvmProperties.size
			llvmProperties.add(constructor.byteType)
		} else if(SpecialType.BYTE_ARRAY.matches(this)) {
			context.byteArrayValueIndex = llvmProperties.size
			llvmProperties.add(constructor.pointerType)
		} else if(SpecialType.INTEGER.matches(this)) {
			context.integerValueIndex = llvmProperties.size
			llvmProperties.add(constructor.i32Type)
		} else if(SpecialType.FLOAT.matches(this)) {
			context.floatValueIndex = llvmProperties.size
			llvmProperties.add(constructor.floatType)
		} else if(SpecialType.NATIVE_INPUT_STREAM.matches(this)) {
			context.nativeInputStreamValueIndex = llvmProperties.size
			llvmProperties.add(constructor.pointerType)
		} else if(SpecialType.NATIVE_OUTPUT_STREAM.matches(this)) {
			context.nativeOutputStreamValueIndex = llvmProperties.size
			llvmProperties.add(constructor.pointerType)
		}
	}

	fun getLlvmReferenceType(constructor: LlvmConstructor): LlvmType {
		if(SpecialType.BOOLEAN.matches(this))
			return constructor.booleanType
		if(SpecialType.BYTE.matches(this))
			return constructor.byteType
		if(SpecialType.INTEGER.matches(this))
			return constructor.i32Type
		if(SpecialType.FLOAT.matches(this))
			return constructor.floatType
		if(SpecialType.NOTHING.matches(this))
			return constructor.voidType
		if(SpecialType.NEVER.matches(this))
			return constructor.voidType
		return constructor.pointerType
	}

	private fun buildLlvmClassInitializer(constructor: LlvmConstructor, staticMembers: List<ValueDeclaration>,
										  properties: List<ValueDeclaration>, functions: List<LlvmMemberFunction>) {
		context.printDebugMessage("'${getFullName()}' class initializer:")
		val previousBlock = constructor.getCurrentBlock()
		constructor.createAndSelectEntrypointBlock(llvmClassInitializer)
		context.printDebugMessage(constructor, "Initializing class '${getFullName()}' with class definition at '%p'.",
			llvmClassDefinition)
		for(typeDeclaration in scope.typeDeclarations.values) {
			if(typeDeclaration.isDefinition)
				constructor.buildFunctionCall(typeDeclaration.llvmClassInitializerType, typeDeclaration.llvmClassInitializer,
					listOf(context.getExceptionParameter(constructor, llvmClassInitializer)))
		}
		val staticMemberCount = constructor.buildInt32(staticMembers.size)
		val staticMemberIdArray = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, staticMemberCount,
			"staticMemberIdArray")
		val staticMemberOffsetArray = constructor.buildHeapArrayAllocation(context.llvmMemberOffsetType, staticMemberCount,
			"staticMemberOffsetArray")
		val propertyCount = constructor.buildInt32(properties.size)
		val propertyIdArray = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, propertyCount, "propertyIdArray")
		val propertyOffsetArray = constructor.buildHeapArrayAllocation(context.llvmMemberOffsetType, propertyCount,
			"propertyOffsetArray")
		val functionCount = constructor.buildInt32(functions.size)
		val functionIdArray = constructor.buildHeapArrayAllocation(context.llvmMemberIdType, functionCount, "functionIdArray")
		val functionAddressArray = constructor.buildHeapArrayAllocation(context.llvmMemberAddressType, functionCount,
			"functionAddressArray")
		val staticMemberOffsets = HashMap<ValueDeclaration, LlvmValue>()
		for((memberIndex, memberDeclaration) in staticMembers.withIndex()) {
			val memberId = context.memberIdentities.getId(memberDeclaration.name)
			val structMemberIndex = memberIndex + FIXED_STATIC_PROPERTY_COUNT
			val memberOffset = constructor.getMemberOffsetInBytes(llvmStaticType, structMemberIndex)
			context.printDebugMessage("Mapping static member '${memberDeclaration.name}' to ID '$memberId' with offset '$memberOffset'.")
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idElement = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, staticMemberIdArray, memberIndexValue,
				"staticMemberIdElement")
			val offsetElement = constructor.buildGetArrayElementPointer(context.llvmMemberOffsetType, staticMemberOffsetArray,
				memberIndexValue, "staticMemberOffsetElement")
			val memberIdValue = constructor.buildInt32(memberId)
			val memberOffsetValue = constructor.buildInt32(memberOffset)
			staticMemberOffsets[memberDeclaration] = memberOffsetValue
			constructor.buildStore(memberIdValue, idElement)
			constructor.buildStore(memberOffsetValue, offsetElement)
		}
		var fixedPropertyCount = FIXED_PROPERTY_COUNT
		if(isBound)
			fixedPropertyCount++
		for((memberIndex, property) in properties.withIndex()) {
			val memberId = context.memberIdentities.getId(property.name)
			val structMemberIndex = memberIndex + fixedPropertyCount
			val memberOffset = constructor.getMemberOffsetInBytes(llvmType, structMemberIndex)
			context.printDebugMessage("Mapping property '${property.name}' to ID '$memberId' with offset '$memberOffset'.")
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idElement = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, propertyIdArray, memberIndexValue,
				"propertyIdElement")
			val offsetElement = constructor.buildGetArrayElementPointer(context.llvmMemberOffsetType, propertyOffsetArray,
				memberIndexValue, "propertyOffsetElement")
			val memberIdValue = constructor.buildInt32(memberId)
			val memberOffsetValue = constructor.buildInt32(memberOffset)
			constructor.buildStore(memberIdValue, idElement)
			constructor.buildStore(memberOffsetValue, offsetElement)
		}
		for((memberIndex, function) in functions.withIndex()) {
			val memberId = context.memberIdentities.getId(function.identifier)
			context.printDebugMessage("Mapping function '${function.identifier}' to ID '$memberId'.")
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idElement = constructor.buildGetArrayElementPointer(context.llvmMemberIdType, functionIdArray, memberIndexValue,
				"functionIdElement")
			val addressElement = constructor.buildGetArrayElementPointer(context.llvmMemberAddressType, functionAddressArray,
				memberIndexValue, "functionAddressElement")
			val memberIdValue = constructor.buildInt32(memberId)
			constructor.buildStore(memberIdValue, idElement)
			constructor.buildStore(function.llvmValue, addressElement)
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
		constructor.defineGlobal(llvmClassDefinition, constructor.buildConstantStruct(context.classDefinitionStruct, initialStaticValues))
		val staticMemberIdArrayProperty = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinition,
			Context.CONSTANT_ID_ARRAY_PROPERTY_INDEX, "staticMemberIdArrayProperty")
		val staticMemberOffsetArrayProperty = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinition,
			Context.CONSTANT_OFFSET_ARRAY_PROPERTY_INDEX, "staticMemberOffsetArrayProperty")
		val propertyIdArrayProperty = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinition,
			Context.PROPERTY_ID_ARRAY_PROPERTY_INDEX, "propertyIdArrayProperty")
		val propertyOffsetArrayProperty = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinition,
			Context.PROPERTY_OFFSET_ARRAY_PROPERTY_INDEX, "propertyOffsetArrayProperty")
		val functionIdArrayProperty = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinition,
			Context.FUNCTION_ID_ARRAY_PROPERTY_INDEX, "functionIdArrayProperty")
		val functionAddressArrayProperty = constructor.buildGetPropertyPointer(context.classDefinitionStruct, llvmClassDefinition,
			Context.FUNCTION_ADDRESS_ARRAY_PROPERTY_INDEX, "functionAddressArrayProperty")
		constructor.buildStore(staticMemberIdArray, staticMemberIdArrayProperty)
		constructor.buildStore(staticMemberOffsetArray, staticMemberOffsetArrayProperty)
		constructor.buildStore(propertyIdArray, propertyIdArrayProperty)
		constructor.buildStore(propertyOffsetArray, propertyOffsetArrayProperty)
		constructor.buildStore(functionIdArray, functionIdArrayProperty)
		constructor.buildStore(functionAddressArray, functionAddressArrayProperty)
		if(this !is Object) {
			val staticObject = staticValueDeclaration.llvmLocation
			val values = LinkedList<LlvmValue>()
			values.add(llvmClassDefinition)
			for(staticMember in staticMembers) {
				if(staticMember is Instance) {
					values.add(context.getNullValue(constructor, staticMember.effectiveType))
					if(staticMember.isAbstract)
						continue
					val offset = staticMemberOffsets[staticMember]
						?: throw CompilerError(staticMember.source, "Missing static member offset.")
					val staticProperty = constructor.buildGetArrayElementPointer(constructor.byteType, staticObject, offset,
						"_staticMemberAddress")
					constructor.buildStore(staticMember.getLlvmValue(constructor), staticProperty)
					continue
				}
				val staticMemberValue = staticMember.value?.getComputedValue()
				val staticMemberLlvmValue = if(staticMemberValue?.providedType?.isLlvmPrimitive() == true) {
					staticMemberValue.getLlvmValue(constructor)
				} else {
					val value = staticMember.value?.getLlvmValue(constructor)
						?: throw CompilerError(staticMember.source, "Static member is missing a value.")
					val offset = staticMemberOffsets[staticMember]
						?: throw CompilerError(staticMember.source, "Missing static member offset.")
					val staticProperty = constructor.buildGetArrayElementPointer(constructor.byteType, staticObject, offset,
						"_staticMemberAddress")
					constructor.buildStore(value, staticProperty)
					constructor.nullPointer
				}
				values.add(staticMemberLlvmValue)
			}
			constructor.defineGlobal(staticObject, constructor.buildConstantStruct(llvmStaticType, values))
		}
		context.printDebugMessage(constructor, "Class '${getFullName()}' initialized.")
		constructor.buildReturn()
		constructor.select(previousBlock)
	}

	private fun buildLlvmCommonPreInitializer(constructor: LlvmConstructor, properties: List<ValueDeclaration>) {
		val previousBlock = constructor.getCurrentBlock()
		constructor.createAndSelectEntrypointBlock(llvmCommonPreInitializer)
		val exceptionAddress = constructor.getParameter(llvmCommonPreInitializer, Context.EXCEPTION_PARAMETER_INDEX)
		val thisValue = constructor.getParameter(llvmCommonPreInitializer, Context.THIS_PARAMETER_INDEX)
		context.printDebugMessage(constructor, "Running '${getFullName()}' pre-initialization of object at '%p'.", thisValue)
		if(isBound) {
			val parentValue = constructor.getParameter(llvmCommonPreInitializer, Context.PARENT_PARAMETER_OFFSET)
			val parentProperty = constructor.buildGetPropertyPointer(llvmType, thisValue, Context.PARENT_PROPERTY_INDEX,
				"_parentProperty")
			constructor.buildStore(parentValue, parentProperty)
		}
		for(genericTypeDeclaration in scope.getGenericTypeDeclarations()) {
			val propertyAddress = context.resolveMember(constructor, thisValue, genericTypeDeclaration.name)
			constructor.buildStore(constructor.getParameter(llvmCommonPreInitializer, genericTypeDeclaration.index), propertyAddress)
		}
		for(superType in getDirectSuperTypes()) {
			if(SpecialType.IDENTIFIABLE.matches(superType) || SpecialType.ANY.matches(superType))
				continue
			val typeDeclaration = superType.getTypeDeclaration() ?: throw CompilerError(superType.source,
				"Object type missing type declaration.")
			val parameters = LinkedList<LlvmValue?>()
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionAddress)
			parameters.add(Context.THIS_PARAMETER_INDEX, thisValue)
			for(typeParameter in superType.typeParameters) {
				val objectType = typeParameter.effectiveType as? ObjectType
					?: throw CompilerError(typeParameter.source, "Only object types are allowed as type parameters.")
				parameters.add(objectType.getStaticLlvmValue(constructor))
			}
			constructor.buildFunctionCall(typeDeclaration.llvmCommonPreInitializerType, typeDeclaration.llvmCommonPreInitializer,
				parameters)
			context.continueRaise(constructor, parent)
		}
		for(memberDeclaration in properties) {
			val memberValue = memberDeclaration.value
			if(memberValue != null) {
				val convertedValue = ValueConverter.convertIfRequired(memberDeclaration, constructor,
					memberValue.buildLlvmValue(constructor), memberValue.effectiveType, memberValue.hasGenericType,
					memberDeclaration.effectiveType,
					false, memberDeclaration.conversion)
				val memberAddress = context.resolveMember(constructor, thisValue, memberDeclaration.name)
				constructor.buildStore(convertedValue, memberAddress)
			}
		}
		context.printDebugMessage(constructor, "Finished '${getFullName()}' pre-initialization of object at '%p'.", thisValue)
		constructor.buildReturn()
		constructor.select(previousBlock)
	}

	fun getLlvmMetadata(constructor: LlvmConstructor): LlvmDebugInfoMetadata {
		var llvmMetadata = cachedLlvmMetadata
		if(llvmMetadata == null) {
			val file = constructor.debug.createFile("test.pure", ".")
			llvmMetadata = constructor.debug.createTypeDeclaration(file, name, file, 0, 0, 0)
			this.cachedLlvmMetadata = llvmMetadata
		}
		return llvmMetadata
	}

	fun isLlvmPrimitive(): Boolean {
		return SpecialType.BOOLEAN.matches(this)
			|| SpecialType.BYTE.matches(this)
			|| SpecialType.INTEGER.matches(this)
			|| SpecialType.FLOAT.matches(this)
	}

	override fun toString(): String {
		if(superType == null || SpecialType.ANY.matches(superType))
			return name
		return "$name: $superType"
	}

	fun getFullName(): String {
		val parentTypeDeclaration = parentTypeDeclaration ?: return name
		return "${parentTypeDeclaration.getFullName()}.$name"

	}

	class LlvmMemberFunction(val identifier: String, val llvmValue: LlvmValue)
}
