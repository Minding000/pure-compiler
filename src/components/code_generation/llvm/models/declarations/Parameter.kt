package components.code_generation.llvm.models.declarations

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.declarations.Parameter
import kotlin.properties.Delegates

class Parameter(override val model: Parameter): ValueDeclaration(model) {
	var index by Delegates.notNull<Int>()

	override fun compile(constructor: LlvmConstructor) {
		if(model.isVariadic)
			return
		val function = constructor.getParentFunction()
		val value = constructor.getParameter(function, index)
		llvmLocation = constructor.buildStackAllocation(constructor.getParameterType(function, index), model.name, value)
	}
}
