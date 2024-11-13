package components.code_generation.llvm.models.general

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.context.Context
import components.semantic_model.context.VariableTracker
import components.semantic_model.general.SemanticModel

abstract class Unit(open val model: SemanticModel, val units: List<Unit> = emptyList()) {
	val context: Context
		get() = model.context
	var parent: Unit? = null
	var hasDeterminedFileInitializationOrder = false

	init {
		for(unit in units)
			unit.parent = this
	}

	open fun declare() {
		for(unit in units)
			unit.declare()
	}

	open fun determineTypes() {
		for(unit in units)
			unit.determineTypes()
	}

	open fun analyseDataFlow(tracker: VariableTracker) {
		for(unit in units)
			unit.analyseDataFlow(tracker)
	}

	open fun validate() {
		for(unit in units)
			unit.validate()
	}

	open fun declare(constructor: LlvmConstructor) {
		for(unit in units)
			unit.declare(constructor)
	}

	open fun define(constructor: LlvmConstructor) {
		for(unit in units)
			unit.define(constructor)
	}

	open fun compile(constructor: LlvmConstructor) {
		for(unit in units)
			unit.compile(constructor)
	}

	inline fun <reified T: Unit> getSurrounding(): T? {
		if(this is T)
			return this
		return parent?.getSurrounding(T::class.java)
	}

	fun <T: Unit> getSurrounding(`class`: Class<T>): T? {
		@Suppress("UNCHECKED_CAST") // Cast will always work, because Class<T> == T
		if(`class`.isAssignableFrom(this.javaClass))
			return this as T
		return parent?.getSurrounding(`class`)
	}

	open fun determineFileInitializationOrder(filesToInitialize: LinkedHashSet<File>) {
		//println("Checking '${javaClass.simpleName}' '$this' in '${getSurrounding<File>()?.file?.name}'")
		//if(this is PropertyDeclaration)
		//	println("Property declaration: $name")
		if(hasDeterminedFileInitializationOrder)
			return
		hasDeterminedFileInitializationOrder = true
		for(unit in units)
			unit.determineFileInitializationOrder(filesToInitialize)
	}
}
