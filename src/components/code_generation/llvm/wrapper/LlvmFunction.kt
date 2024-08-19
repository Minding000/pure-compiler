package components.code_generation.llvm.wrapper

class LlvmFunction(val value: LlvmValue, val type: LlvmType) {

	constructor(constructor: LlvmConstructor, name: String, type: LlvmType): this(constructor.buildFunction(name, type), type)

	constructor(constructor: LlvmConstructor, name: String, parameterTypes: List<LlvmType?>, returnType: LlvmType = constructor.voidType,
				isVariadic: Boolean = false):
		this(constructor, name, constructor.buildFunctionType(parameterTypes, returnType, isVariadic))
}
