package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.StandardLibrary
import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.models.values.FunctionObject
import components.code_generation.llvm.wrapper.*
import components.semantic_model.context.Context
import components.semantic_model.context.Context.Companion.CLASS_DEFINITION_PROPERTY_INDEX
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.InterfaceMember
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.types.ObjectType
import errors.internal.CompilerError
import java.util.*

abstract class TypeDeclaration(override val model: TypeDeclaration, val members: List<Unit>,
							   val staticValueDeclaration: ValueDeclaration? = null):
	Unit(model, listOfNotNull(staticValueDeclaration, *members.toTypedArray())) {
	lateinit var staticMembers: List<ValueDeclaration>
	lateinit var properties: List<ValueDeclaration>
	private lateinit var functions: List<LlvmMemberFunction>
	lateinit var llvmType: LlvmType
	lateinit var llvmClassDefinition: LlvmValue
	lateinit var classInitializer: LlvmFunction
	lateinit var commonClassPreInitializer: LlvmFunction
	lateinit var llvmStaticType: LlvmType
	private val staticMemberOffsets = HashMap<ValueDeclaration, LlvmValue>()
	private var cachedLlvmMetadata: LlvmDebugInfoMetadata? = null

	companion object {
		const val FIXED_STATIC_PROPERTY_COUNT = 1
		const val FIXED_PROPERTY_COUNT = 1
	}

	override fun declare(constructor: LlvmConstructor) {
		if(model.isDefinition) {
			declareClassInitializer(constructor)
			declareCommonPreInitializer(constructor)
			llvmStaticType = constructor.declareStruct("${model.getFullName()}_StaticStruct")
			llvmType = constructor.declareStruct("${model.getFullName()}_ClassStruct")
			llvmClassDefinition =
				constructor.declareGlobal("${model.getFullName()}_ClassDefinition", context.runtimeStructs.classDefinition)
			staticValueDeclaration?.llvmLocation = constructor.declareGlobal("${model.getFullName()}_StaticObject", llvmStaticType)
		}
		super.declare(constructor)
	}

	private fun declareClassInitializer(constructor: LlvmConstructor) {
		classInitializer = LlvmFunction(constructor, "${model.getFullName()}_ClassInitializer", listOf(constructor.pointerType))
	}

	private fun declareCommonPreInitializer(constructor: LlvmConstructor) {
		val parameterTypes = LinkedList<LlvmType?>()
		parameterTypes.add(Context.EXCEPTION_PARAMETER_INDEX, constructor.pointerType)
		parameterTypes.add(Context.THIS_PARAMETER_INDEX, constructor.pointerType)
		var parameterIndex = Context.VALUE_PARAMETER_OFFSET
		if(model.isBound) {
			parameterTypes.add(Context.PARENT_PARAMETER_OFFSET, constructor.pointerType)
			parameterIndex++
		}
		for(genericTypeDeclaration in model.scope.getGenericTypeDeclarations()) {
			parameterTypes.add(constructor.pointerType)
			genericTypeDeclaration.index = parameterIndex
			parameterIndex++
		}
		commonClassPreInitializer = LlvmFunction(constructor, "${model.getFullName()}_CommonPreInitializer", parameterTypes)
	}

	override fun define(constructor: LlvmConstructor) {
		if(model.isDefinition) {
			staticMembers = getStaticMembers().values.toList()
			properties = getProperties().values.toList()
			functions = getFunctions().values.toList()
			defineLlvmStruct(constructor, staticMembers, properties)
			for(memberDeclaration in staticMembers)
				context.memberIdentities.register(memberDeclaration.model.name)
			for(property in properties)
				context.memberIdentities.register(property.model.name)
			for(function in functions)
				context.memberIdentities.register(function.identifier)
		}
		super.define(constructor)
	}

	override fun compile(constructor: LlvmConstructor) {
		if(model.isDefinition) {
			buildLlvmClassInitializer(constructor, staticMembers, properties, functions)
			buildLlvmCommonPreInitializer(constructor, properties)
			if(staticValueDeclaration != null)
				compileStaticMemberInitialization(constructor, staticValueDeclaration.llvmLocation)
		}
		super.compile(constructor)
	}

	/**
	 * Returns instances and static properties (e.g. 'staticInstances')
	 */
	private fun getStaticMembers(): Map<String, ValueDeclaration> {
		val staticMembers = HashMap<String, ValueDeclaration>()
		for(member in members) {
			if(member is Instance || (member is PropertyDeclaration && member.model.isStatic && member.model.name == "staticInstances"))
				staticMembers[member.model.name] = member
		}
		for(superType in model.getDirectSuperTypes()) {
			for(staticMember in superType.getTypeDeclaration()?.unit?.getStaticMembers()?.values ?: emptyList())
				staticMembers.putIfAbsent(staticMember.model.name, staticMember)
		}
		return staticMembers
	}

	/**
	 * Returns non-computed properties and generic type definitions
	 */
	private fun getProperties(): Map<String, ValueDeclaration> {
		val properties = HashMap<String, ValueDeclaration>()
		for(member in members) {
			if(member is ValueDeclaration && member !is ComputedPropertyDeclaration && member.value !is FunctionObject
				&& (member.model as? InterfaceMember)?.isStatic != true)
				properties[member.model.name] = member
			if(member is GenericTypeDeclaration && member.staticValueDeclaration != null)
				properties[member.model.name] = member.staticValueDeclaration
		}
		for(superType in model.getDirectSuperTypes()) {
			for(property in superType.getTypeDeclaration()?.unit?.getProperties()?.values ?: emptyList())
				properties.putIfAbsent(property.model.name, property)
		}
		return properties
	}

	/**
	 * Returns functions, operators and computed properties
	 */
	private fun getFunctions(): Map<String, LlvmMemberFunction> {
		val functions = HashMap<String, LlvmMemberFunction>()
		for(member in members) {
			if(member is PropertyDeclaration && member.value is FunctionObject) {
				for(definition in member.value.definitions) {
					val memberIdentifier = definition.model.memberIdentifier
					if(!definition.model.isAbstract)
						functions[memberIdentifier] = LlvmMemberFunction(memberIdentifier, definition.llvmValue)
				}
			}
			if(member is ComputedPropertyDeclaration) {
				val llvmGetterValue = member.llvmGetterValue
				if(llvmGetterValue != null) {
					val getterIdentifier = member.model.getterIdentifier
					if(!member.model.isAbstract)
						functions[getterIdentifier] = LlvmMemberFunction(getterIdentifier, llvmGetterValue)
				}
				val llvmSetterValue = member.llvmSetterValue
				if(llvmSetterValue != null) {
					val setterIdentifier = member.model.setterIdentifier
					if(!member.model.isAbstract)
						functions[setterIdentifier] = LlvmMemberFunction(setterIdentifier, llvmSetterValue)
				}
			}
		}
		for(superType in model.getDirectSuperTypes()) {
			for(function in superType.getTypeDeclaration()?.unit?.getFunctions()?.values ?: emptyList())
				functions.putIfAbsent(function.identifier, function)
		}
		return functions
	}

	private fun defineLlvmStruct(constructor: LlvmConstructor, staticMembers: List<ValueDeclaration>, properties: List<ValueDeclaration>) {
		val llvmConstants = LinkedList<LlvmType?>()
		llvmConstants.add(constructor.pointerType)
		for(memberDeclaration in staticMembers)
			llvmConstants.add(memberDeclaration.model.effectiveType?.getLlvmType(constructor))
		constructor.defineStruct(llvmStaticType, llvmConstants)
		val llvmProperties = LinkedList<LlvmType?>()
		llvmProperties.add(constructor.pointerType)
		if(model.isBound)
			llvmProperties.add(constructor.pointerType)
		for(memberDeclaration in properties)
			llvmProperties.add(memberDeclaration.model.effectiveType?.getLlvmType(constructor))
		addNativeProperties(constructor, llvmProperties)
		constructor.defineStruct(llvmType, llvmProperties)
	}

	private fun addNativeProperties(constructor: LlvmConstructor, llvmProperties: LinkedList<LlvmType?>) {
		if(SpecialType.ARRAY.matches(model)) {
			context.standardLibrary.array = StandardLibrary.NativeRuntimeClass(this, llvmProperties.size)
			llvmProperties.add(constructor.pointerType)
		} else if(SpecialType.BOOLEAN.matches(model)) {
			context.standardLibrary.boolean = StandardLibrary.NativeRuntimeClass(this, llvmProperties.size)
			llvmProperties.add(constructor.booleanType)
		} else if(SpecialType.BYTE.matches(model)) {
			context.standardLibrary.byte = StandardLibrary.NativeRuntimeClass(this, llvmProperties.size)
			llvmProperties.add(constructor.byteType)
		} else if(SpecialType.BYTE_ARRAY.matches(model)) {
			context.standardLibrary.byteArrayTypeDeclaration = this
			context.standardLibrary.byteArray = StandardLibrary.NativeRuntimeClass(this, llvmProperties.size)
			llvmProperties.add(constructor.pointerType)
		} else if(SpecialType.INTEGER.matches(model)) {
			context.standardLibrary.integer = StandardLibrary.NativeRuntimeClass(this, llvmProperties.size)
			llvmProperties.add(constructor.i32Type)
		} else if(SpecialType.FLOAT.matches(model)) {
			context.standardLibrary.float = StandardLibrary.NativeRuntimeClass(this, llvmProperties.size)
			llvmProperties.add(constructor.floatType)
		} else if(SpecialType.NATIVE_INPUT_STREAM.matches(model)) {
			context.standardLibrary.nativeInputStream = StandardLibrary.NativeRuntimeClass(this, llvmProperties.size)
			llvmProperties.add(constructor.pointerType)
		} else if(SpecialType.NATIVE_OUTPUT_STREAM.matches(model)) {
			context.standardLibrary.nativeOutputStream = StandardLibrary.NativeRuntimeClass(this, llvmProperties.size)
			llvmProperties.add(constructor.pointerType)
		}
	}

	fun getLlvmReferenceType(constructor: LlvmConstructor): LlvmType {
		if(SpecialType.BOOLEAN.matches(model))
			return constructor.booleanType
		if(SpecialType.BYTE.matches(model))
			return constructor.byteType
		if(SpecialType.INTEGER.matches(model))
			return constructor.i32Type
		if(SpecialType.FLOAT.matches(model))
			return constructor.floatType
		if(SpecialType.NOTHING.matches(model))
			return constructor.voidType
		if(SpecialType.NEVER.matches(model))
			return constructor.voidType
		return constructor.pointerType
	}

	private fun buildLlvmClassInitializer(constructor: LlvmConstructor, staticMembers: List<ValueDeclaration>,
										  properties: List<ValueDeclaration>, functions: List<LlvmMemberFunction>) {
		context.printDebugMessage("'${model.getFullName()}' class initializer:")
		val previousBlock = constructor.getCurrentBlock()
		constructor.createAndSelectEntrypointBlock(classInitializer.value)
		context.printDebugLine(constructor, "Initializing class '${model.getFullName()}' with class definition at '%p'.",
			llvmClassDefinition)
		for(typeDeclaration in model.scope.typeDeclarations.values) {
			if(typeDeclaration.isDefinition)
				constructor.buildFunctionCall(typeDeclaration.unit.classInitializer, listOf(context.getExceptionParameter(constructor)))
		}
		val staticMemberCount = constructor.buildInt32(staticMembers.size)
		val staticMemberIdArray = constructor.buildHeapArrayAllocation(context.runtimeTypes.memberId, staticMemberCount,
			"staticMemberIdArray")
		val staticMemberOffsetArray = constructor.buildHeapArrayAllocation(context.runtimeTypes.memberOffset, staticMemberCount,
			"staticMemberOffsetArray")
		val propertyCount = constructor.buildInt32(properties.size)
		val propertyIdArray = constructor.buildHeapArrayAllocation(context.runtimeTypes.memberId, propertyCount, "propertyIdArray")
		val propertyOffsetArray = constructor.buildHeapArrayAllocation(context.runtimeTypes.memberOffset, propertyCount,
			"propertyOffsetArray")
		val functionCount = constructor.buildInt32(functions.size)
		val functionIdArray = constructor.buildHeapArrayAllocation(context.runtimeTypes.memberId, functionCount, "functionIdArray")
		val functionAddressArray = constructor.buildHeapArrayAllocation(context.runtimeTypes.memberAddress, functionCount,
			"functionAddressArray")
		for((memberIndex, memberDeclaration) in staticMembers.withIndex()) {
			val memberId = context.memberIdentities.getId(memberDeclaration.model.name)
			val structMemberIndex = memberIndex + FIXED_STATIC_PROPERTY_COUNT
			val memberOffset = constructor.getMemberOffsetInBytes(llvmStaticType, structMemberIndex)
			context.printDebugMessage(
				"Mapping static member '${memberDeclaration.model.name}' to ID '$memberId' with offset '$memberOffset'.")
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idElement = constructor.buildGetArrayElementPointer(context.runtimeTypes.memberId, staticMemberIdArray, memberIndexValue,
				"staticMemberIdElement")
			val offsetElement = constructor.buildGetArrayElementPointer(context.runtimeTypes.memberOffset, staticMemberOffsetArray,
				memberIndexValue, "staticMemberOffsetElement")
			val memberIdValue = constructor.buildInt32(memberId)
			val memberOffsetValue = constructor.buildInt32(memberOffset)
			staticMemberOffsets[memberDeclaration] = memberOffsetValue
			constructor.buildStore(memberIdValue, idElement)
			constructor.buildStore(memberOffsetValue, offsetElement)
		}
		var fixedPropertyCount = FIXED_PROPERTY_COUNT
		if(model.isBound)
			fixedPropertyCount++
		for((memberIndex, property) in properties.withIndex()) {
			val memberId = context.memberIdentities.getId(property.model.name)
			val structMemberIndex = memberIndex + fixedPropertyCount
			val memberOffset = constructor.getMemberOffsetInBytes(llvmType, structMemberIndex)
			context.printDebugMessage("Mapping property '${property.model.name}' to ID '$memberId' with offset '$memberOffset'.")
			val memberIndexValue = constructor.buildInt32(memberIndex)
			val idElement = constructor.buildGetArrayElementPointer(context.runtimeTypes.memberId, propertyIdArray, memberIndexValue,
				"propertyIdElement")
			val offsetElement = constructor.buildGetArrayElementPointer(context.runtimeTypes.memberOffset, propertyOffsetArray,
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
			val idElement = constructor.buildGetArrayElementPointer(context.runtimeTypes.memberId, functionIdArray, memberIndexValue,
				"functionIdElement")
			val addressElement = constructor.buildGetArrayElementPointer(context.runtimeTypes.memberAddress, functionAddressArray,
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
		val classDefinitionStruct = context.runtimeStructs.classDefinition
		constructor.defineGlobal(llvmClassDefinition, constructor.buildConstantStruct(classDefinitionStruct, initialStaticValues))
		val staticMemberIdArrayProperty = constructor.buildGetPropertyPointer(classDefinitionStruct, llvmClassDefinition,
			Context.CONSTANT_ID_ARRAY_PROPERTY_INDEX, "staticMemberIdArrayProperty")
		val staticMemberOffsetArrayProperty = constructor.buildGetPropertyPointer(classDefinitionStruct, llvmClassDefinition,
			Context.CONSTANT_OFFSET_ARRAY_PROPERTY_INDEX, "staticMemberOffsetArrayProperty")
		val propertyIdArrayProperty = constructor.buildGetPropertyPointer(classDefinitionStruct, llvmClassDefinition,
			Context.PROPERTY_ID_ARRAY_PROPERTY_INDEX, "propertyIdArrayProperty")
		val propertyOffsetArrayProperty = constructor.buildGetPropertyPointer(classDefinitionStruct, llvmClassDefinition,
			Context.PROPERTY_OFFSET_ARRAY_PROPERTY_INDEX, "propertyOffsetArrayProperty")
		val functionIdArrayProperty = constructor.buildGetPropertyPointer(classDefinitionStruct, llvmClassDefinition,
			Context.FUNCTION_ID_ARRAY_PROPERTY_INDEX, "functionIdArrayProperty")
		val functionAddressArrayProperty = constructor.buildGetPropertyPointer(classDefinitionStruct, llvmClassDefinition,
			Context.FUNCTION_ADDRESS_ARRAY_PROPERTY_INDEX, "functionAddressArrayProperty")
		constructor.buildStore(staticMemberIdArray, staticMemberIdArrayProperty)
		constructor.buildStore(staticMemberOffsetArray, staticMemberOffsetArrayProperty)
		constructor.buildStore(propertyIdArray, propertyIdArrayProperty)
		constructor.buildStore(propertyOffsetArray, propertyOffsetArrayProperty)
		constructor.buildStore(functionIdArray, functionIdArrayProperty)
		constructor.buildStore(functionAddressArray, functionAddressArrayProperty)
		context.printDebugLine(constructor, "Class '${model.getFullName()}' initialized.")
		constructor.buildReturn()
		constructor.select(previousBlock)
	}

	private fun compileStaticMemberInitialization(constructor: LlvmConstructor, staticObject: LlvmValue) {
		val exceptionParameter = context.getExceptionParameter(constructor)
		val values = LinkedList<LlvmValue>()
		values.add(llvmClassDefinition)
		val instances = HashMap<Instance, LlvmValue>()
		var instanceMap: LlvmValue? = null
		for(staticMember in staticMembers) {
			if(staticMember is Instance) {
				values.add(context.getNullValue(constructor, staticMember.model.effectiveType))
				if(staticMember.model.isAbstract)
					continue
				val offset = staticMemberOffsets[staticMember]
					?: throw CompilerError(staticMember, "Missing static member offset.")
				val staticProperty = constructor.buildGetArrayElementPointer(constructor.byteType, staticObject, offset,
					"_staticMemberAddress")
				val instance = staticMember.getLlvmValue(constructor)
				instances[staticMember] = instance
				constructor.buildStore(instance, staticProperty)
				continue
			}
			val staticMemberValue = staticMember.model.value?.getComputedValue()
			val staticMemberLlvmValue = if(staticMemberValue?.effectiveType?.isLlvmPrimitive() == true) {
				//TODO is 'toUnit' call ok here?
				staticMemberValue.toUnit().getLlvmValue(constructor)
			} else if(staticMember.model.name == "staticInstances") {
				if(context.nativeRegistry.has(SpecialType.MAP)) {
					val mapTypeDeclaration = context.standardLibrary.mapTypeDeclaration
					val map = constructor.buildHeapAllocation(mapTypeDeclaration.llvmType, "staticInstancesMap")
					val mapClassDefinitionProperty =
						constructor.buildGetPropertyPointer(mapTypeDeclaration.llvmType, map,
							CLASS_DEFINITION_PROPERTY_INDEX, "mapClassDefinitionProperty")
					constructor.buildStore(mapTypeDeclaration.llvmClassDefinition, mapClassDefinitionProperty)
					if(!mapTypeDeclaration.commonClassPreInitializer.isNoop) {
						val stringType = context.standardLibrary.stringTypeDeclaration.staticValueDeclaration?.llvmLocation
						val selfType = staticValueDeclaration?.llvmLocation
						constructor.buildFunctionCall(mapTypeDeclaration.commonClassPreInitializer,
							listOf(exceptionParameter, map, stringType,
								selfType)) //TODO requires target pre-initializer to be built already
					}
					val parameters = listOf(exceptionParameter, map)
					constructor.buildFunctionCall(context.standardLibrary.mapInitializer, parameters)
					instanceMap = map

					val offset = staticMemberOffsets[staticMember]
						?: throw CompilerError(staticMember, "Missing static member offset for '${staticMember.model.name}'.")
					val staticProperty = constructor.buildGetArrayElementPointer(constructor.byteType, staticObject, offset,
						"_staticMemberAddress")
					constructor.buildStore(map, staticProperty)
				}
				constructor.nullPointer
			} else {
				val value = staticMember.value?.getLlvmValue(constructor)
					?: throw CompilerError(staticMember, "Static member '${staticMember.model.name}' is missing a value.")
				val offset = staticMemberOffsets[staticMember]
					?: throw CompilerError(staticMember, "Missing static member offset for '${staticMember.model.name}'.")
				val staticProperty = constructor.buildGetArrayElementPointer(constructor.byteType, staticObject, offset,
					"_staticMemberAddress")
				constructor.buildStore(value, staticProperty)
				constructor.nullPointer
			}
			values.add(staticMemberLlvmValue)
		}
		if(context.nativeRegistry.has(SpecialType.MAP)) {
			for((instance, instanceValue) in instances) {
				val map = instanceMap ?: throw CompilerError(model, "Missing static instance map")
				val mapSetterFunctionAddress = context.resolveFunction(constructor, map, "[Key](Value)")
				val instanceNameString = context.createStringObject(constructor, instance.model.name, exceptionParameter)
				val convertedInstanceValue =
					ValueConverter.convertIfRequired(model, constructor, instanceValue, instance.model.effectiveType, false,
						instance.model.effectiveType, true)
				constructor.buildFunctionCall(context.standardLibrary.mapSetterFunctionType, mapSetterFunctionAddress,
					listOf(exceptionParameter, map, instanceNameString, convertedInstanceValue))
			}
		}
		constructor.defineGlobal(staticObject, constructor.buildConstantStruct(llvmStaticType, values))
	}

	private fun buildLlvmCommonPreInitializer(constructor: LlvmConstructor, properties: List<ValueDeclaration>) {
		var isNoop = true
		val previousBlock = constructor.getCurrentBlock()
		constructor.createAndSelectEntrypointBlock(commonClassPreInitializer.value)
		val exceptionParameter = context.getExceptionParameter(constructor)
		val thisValue = constructor.getParameter(Context.THIS_PARAMETER_INDEX)
		context.printDebugLine(constructor, "Running '${model.getFullName()}' pre-initialization of object at '%p'.", thisValue)
		if(model.isBound) {
			val parentValue = constructor.getParameter(Context.PARENT_PARAMETER_OFFSET)
			val parentProperty = constructor.buildGetPropertyPointer(llvmType, thisValue, Context.PARENT_PROPERTY_INDEX,
				"_parentProperty")
			constructor.buildStore(parentValue, parentProperty)
			isNoop = false
		}
		for(genericTypeDeclaration in model.scope.getGenericTypeDeclarations()) {
			val propertyAddress = context.resolveMember(constructor, thisValue, genericTypeDeclaration.name)
			constructor.buildStore(constructor.getParameter(genericTypeDeclaration.index), propertyAddress)
			isNoop = false
		}
		for(superType in model.getDirectSuperTypes()) {
			if(SpecialType.IDENTIFIABLE.matches(superType) || SpecialType.ANY.matches(superType))
				continue
			val typeDeclaration = superType.getTypeDeclaration()?.unit ?: throw CompilerError(superType.source,
				"Object type missing type declaration.")
			val parameters = LinkedList<LlvmValue?>()
			parameters.add(Context.EXCEPTION_PARAMETER_INDEX, exceptionParameter)
			parameters.add(Context.THIS_PARAMETER_INDEX, thisValue)
			for(typeParameter in superType.typeParameters) {
				val objectType = typeParameter.effectiveType as? ObjectType
					?: throw CompilerError(typeParameter.source, "Only object types are allowed as type parameters.")
				parameters.add(objectType.getStaticLlvmValue(constructor))
			}
			if(typeDeclaration.commonClassPreInitializer.isNoop) //TODO requires target pre-initializer to be built already
				continue
			constructor.buildFunctionCall(typeDeclaration.commonClassPreInitializer, parameters)
			context.continueRaise(constructor, model)
			isNoop = false
		}
		for(memberDeclaration in properties) {
			if(memberDeclaration.parent != this)
				continue
			val memberValue = memberDeclaration.value ?: continue
			val convertedValue = ValueConverter.convertIfRequired(memberDeclaration.model, constructor,
				memberValue.buildLlvmValue(constructor), memberValue.model.effectiveType, memberValue.model.hasGenericType,
				memberDeclaration.model.effectiveType, false, memberDeclaration.model.conversion)
			val memberAddress = context.resolveMember(constructor, thisValue, memberDeclaration.model.name)
			constructor.buildStore(convertedValue, memberAddress)
			isNoop = false
		}
		commonClassPreInitializer.isNoop = isNoop
		context.printDebugLine(constructor, "Finished '${model.getFullName()}' pre-initialization of object at '%p'.", thisValue)
		constructor.buildReturn()
		constructor.select(previousBlock)
	}

	fun getLlvmMetadata(constructor: LlvmConstructor): LlvmDebugInfoMetadata {
		var llvmMetadata = cachedLlvmMetadata
		if(llvmMetadata == null) {
			val file = constructor.debug.createFile("test.pure", ".")
			llvmMetadata = constructor.debug.createTypeDeclaration(file, model.name, file, 0, 0, 0)
			this.cachedLlvmMetadata = llvmMetadata
		}
		return llvmMetadata
	}

	fun isLlvmPrimitive(): Boolean {
		return SpecialType.BOOLEAN.matches(model)
			|| SpecialType.BYTE.matches(model)
			|| SpecialType.INTEGER.matches(model)
			|| SpecialType.FLOAT.matches(model)
	}

	class LlvmMemberFunction(val identifier: String, val llvmValue: LlvmValue)
}
