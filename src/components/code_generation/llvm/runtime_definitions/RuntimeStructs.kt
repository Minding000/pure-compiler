package components.code_generation.llvm.runtime_definitions

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmType
import components.semantic_model.general.Program.Companion.RUNTIME_PREFIX

class RuntimeStructs {
	lateinit var classDefinition: LlvmType
	lateinit var closure: LlvmType
	lateinit var variadicParameterList: LlvmType

	fun declare(constructor: LlvmConstructor) {
		declareClassDefinitionStruct(constructor)
		declareClosureStruct(constructor)
		declareVariadicParameterListStruct(constructor)
	}

	private fun declareClassDefinitionStruct(constructor: LlvmConstructor) {
		classDefinition = constructor.declareStruct("${RUNTIME_PREFIX}ClassStruct")
		// The member count is not strictly required because the loop is guaranteed to find a matching member,
		//  but it is included for debugging and error reporting.
		val memberCountType = constructor.i32Type
		val memberIdArrayType = constructor.pointerType
		val memberOffsetArrayType = constructor.pointerType
		val memberAddressArrayType = constructor.pointerType
		val classDefinitionMemberTypes = listOf(
			memberCountType, memberIdArrayType, memberOffsetArrayType,
			memberCountType, memberIdArrayType, memberOffsetArrayType,
			memberCountType, memberIdArrayType, memberAddressArrayType
		)
		constructor.defineStruct(classDefinition, classDefinitionMemberTypes)
	}

	private fun declareClosureStruct(constructor: LlvmConstructor) {
		closure = constructor.declareStruct("${RUNTIME_PREFIX}Closure")
		constructor.defineStruct(closure, listOf(constructor.pointerType, constructor.i32Type, constructor.pointerType))
	}

	private fun declareVariadicParameterListStruct(constructor: LlvmConstructor) {
		variadicParameterList = constructor.declareStruct("${RUNTIME_PREFIX}variadicParameterList")
		val targetTriple = constructor.getTargetTriple()
		val variadicParameterListStructMembers = if(targetTriple.contains("x86_64-unknown-linux"))
			listOf(constructor.i32Type, constructor.i32Type, constructor.pointerType, constructor.pointerType)
		else
			listOf(constructor.pointerType)
		constructor.defineStruct(variadicParameterList, variadicParameterListStructMembers)
	}
}
