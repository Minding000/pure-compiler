package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.literals.Identifier
import messages.Message

open class VariableValue(override val source: Element, val name: String): Value(source) {
	var definition: ValueDeclaration? = null

	constructor(source: Identifier): this(source, source.getValue())

	override fun linkValues(linter: Linter, scope: Scope) {
		val definition = scope.resolveValue(this)
		if(definition == null) {
			linter.addMessage(source, "Value '$name' hasn't been declared yet.", Message.Type.ERROR)
			return
		}
		definition.usages.add(this)
		this.definition = definition
		type = definition.type
		if(definition.isConstant)
			staticValue = definition.value?.staticValue
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		val usage = tracker.add(VariableUsage.Type.READ, this) ?: return
		val declaration = definition
		if(declaration is LocalVariableDeclaration) {
			if(!usage.isPreviouslyInitialized())
				linter.addMessage(source, "Local variable '$name' hasn't been initialized yet.", Message.Type.ERROR)
		} else if(declaration is PropertyDeclaration) {
			if(tracker.isInitializer && declaration.value == null && !usage.isPreviouslyInitialized())
				linter.addMessage(source, "Property '$name' hasn't been initialized yet.", Message.Type.ERROR)
		}
	}

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + (definition?.hashCode() ?: 0)
		return result
	}

	override fun equals(other: Any?): Boolean {
		if(other !is VariableValue)
			return false
		if(definition == null)
			return false
		return definition == other.definition
	}

	override fun toString(): String = name
}
