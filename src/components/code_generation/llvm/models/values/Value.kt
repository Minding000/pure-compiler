package components.code_generation.llvm.models.values

import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.values.Value
import errors.internal.CompilerError

open class Value(override val model: Value, units: List<Unit> = emptyList()): Unit(model, units) {
	private var llvmValue: LlvmValue? = null

	open fun getLlvmLocation(constructor: LlvmConstructor): LlvmValue? {
		throw CompilerError(model, "Tried to access '${javaClass.simpleName}' LLVM location.")
	}

	fun getLlvmValue(constructor: LlvmConstructor): LlvmValue {
		var llvmValue = llvmValue
		if(llvmValue == null) {
			llvmValue = buildLlvmValue(constructor)
			this.llvmValue = llvmValue
		}
		return llvmValue
	}

	override fun compile(constructor: LlvmConstructor) {
		// In case it is not used as a value
		buildLlvmValue(constructor)
	}

	open fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		TODO("${model.source.getStartString()}: '${javaClass.simpleName}.buildLlvmValue' is not implemented yet.")
	}
}
