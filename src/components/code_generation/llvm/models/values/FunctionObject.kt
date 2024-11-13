package components.code_generation.llvm.models.values

import components.code_generation.llvm.models.declarations.FunctionDefinition
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.values.Function

class FunctionObject(override val model: Function, val definitions: List<FunctionDefinition>): Value(model, definitions) {

	override fun compile(constructor: LlvmConstructor) {
		//TODO compile closures (write tests!)
		for(definition in definitions)
			definition.compile(constructor)
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		TODO("Not implemented yet")
	}
}
