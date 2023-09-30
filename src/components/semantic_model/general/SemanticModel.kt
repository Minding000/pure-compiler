package components.semantic_model.general

import components.code_generation.llvm.LlvmConstructor
import components.semantic_model.context.Context
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.declaration.DuplicateChildModel
import java.util.*

abstract class SemanticModel(open val source: SyntaxTreeNode, open val scope: Scope) {
	val context: Context
		get() = source.context
	open var parent: SemanticModel? = null
	val semanticModels = LinkedList<SemanticModel>()
	open val isInterruptingExecution = false

	fun addSemanticModels(vararg newSemanticModels: SemanticModel?) {
		newModel@for(newSemanticModel in newSemanticModels) {
			if(newSemanticModel != null) {
				newSemanticModel.parent = this
				for(existingSemanticModel in semanticModels) {
					if(existingSemanticModel === newSemanticModel) {
						context.addIssue(DuplicateChildModel(newSemanticModel))
						continue@newModel
					}
				}
				semanticModels.add(newSemanticModel)
			}
		}
	}

	fun addSemanticModels(vararg newSemanticModels: Collection<SemanticModel?>) {
		for(semanticModelCollection in newSemanticModels) {
			for(newSemanticModel in semanticModelCollection)
				addSemanticModels(newSemanticModel)
		}
	}

	fun removeSemanticModel(semanticModel: SemanticModel) {
		semanticModels.remove(semanticModel)
		semanticModel.parent = null
	}

	fun contains(other: SemanticModel): Boolean {
		return source.start < other.source.start && other.source.end < source.end
	}

	fun isIn(other: SemanticModel): Boolean {
		return this === other || parent?.isIn(other) ?: false
	}

	fun isBefore(other: SemanticModel): Boolean {
		return source.end < other.source.start
	}

	fun isAfter(other: SemanticModel): Boolean {
		return other.source.end < source.start
	}

	open fun declare() {
		for(semanticModel in semanticModels)
			semanticModel.declare()
	}

	open fun determineTypes() {
		for(semanticModel in semanticModels)
			semanticModel.determineTypes()
	}

	open fun analyseDataFlow(tracker: VariableTracker) {
		for(semanticModel in semanticModels)
			semanticModel.analyseDataFlow(tracker)
	}

	open fun validate() {
		for(semanticModel in semanticModels)
			semanticModel.validate()
	}

	open fun declare(constructor: LlvmConstructor) {
		for(semanticModel in semanticModels)
			semanticModel.declare(constructor)
	}

	open fun define(constructor: LlvmConstructor) {
		for(semanticModel in semanticModels)
			semanticModel.define(constructor)
	}

	open fun compile(constructor: LlvmConstructor) {
		for(semanticModel in semanticModels)
			semanticModel.compile(constructor)
	}

	fun isInInitializer(): Boolean {
		if(this is InitializerDefinition)
			return true
		if(this is FunctionImplementation)
			return false
		return parent?.isInInitializer() ?: return false
	}

	inline fun <reified T: SemanticModel>find(noinline predicate: (T) -> Boolean): T? {
		if(this is T && predicate(this))
			return this
		for(semanticModel in semanticModels)
			return semanticModel.find(predicate, T::class.java) ?: continue
		return null
	}

	fun <T: SemanticModel>find(predicate: (T) -> Boolean, `class`: Class<T>): T? {
		@Suppress("UNCHECKED_CAST") // Cast will always work, because Class<T> == T
		if(`class`.isAssignableFrom(this.javaClass) && predicate(this as T))
			return this
		for(semanticModel in semanticModels)
			return semanticModel.find(predicate, `class`) ?: continue
		return null
	}
}
