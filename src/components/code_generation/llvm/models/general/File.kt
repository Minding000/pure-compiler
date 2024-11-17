package components.code_generation.llvm.models.general

import components.code_generation.llvm.models.declarations.TypeDeclaration
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmFunction
import java.util.*
import components.semantic_model.general.File as SemanticFileModel

class File(override val model: SemanticFileModel):
	Unit(model, model.semanticModels.mapNotNull { semanticModel -> semanticModel.toUnit() }) {
	val name: String
		get() = model.file.name
	var requiresFileRunner = false
	lateinit var initializer: LlvmFunction
	lateinit var runner: LlvmFunction

	override fun declare(constructor: LlvmConstructor) {
		super.declare(constructor)
		initializer = LlvmFunction(constructor, "${name}_FileInitializer", listOf(constructor.pointerType))
		runner = LlvmFunction(constructor, "${name}_FileRunner", listOf(constructor.pointerType))
	}

	override fun compile(constructor: LlvmConstructor) {
		compileInitializer(constructor)
		compileRunner(constructor)
	}

	private fun compileInitializer(constructor: LlvmConstructor) {
		constructor.createAndSelectEntrypointBlock(initializer.value)
		context.printDebugLine(constructor, "Initializing file '$name'.")
		val exceptionParameter = context.getExceptionParameter(constructor)
		for(unit in units) {
			if(unit is TypeDeclaration && unit.model.isDefinition) {
				constructor.buildFunctionCall(unit.classInitializer, listOf(exceptionParameter))
				context.continueRaise(constructor, model)
			}
		}
		context.printDebugLine(constructor, "File '$name' initialized.")
		constructor.buildReturn()
	}

	private fun compileRunner(constructor: LlvmConstructor) {
		constructor.createAndSelectEntrypointBlock(runner.value)
		context.printDebugLine(constructor, "Executing file '$name'.")
		super.compile(constructor)
		context.printDebugLine(constructor, "File '$name' executed.")
		constructor.buildReturn()
	}

	override fun determineFileInitializationOrder(filesToInitialize: LinkedList<File>) {
		if(hasDeterminedFileInitializationOrder)
			return
		super.determineFileInitializationOrder(filesToInitialize)
		if(requiresFileRunner)
			filesToInitialize.add(this)
	}
}
