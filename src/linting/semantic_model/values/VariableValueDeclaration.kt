package linting.semantic_model.values

import linting.Linter
import linting.semantic_model.types.Type
import linting.semantic_model.general.Unit
import linting.semantic_model.types.ObjectType
import linting.semantic_model.scopes.Scope
import messages.Message
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.literals.Identifier

open class VariableValueDeclaration(override val source: Element, val name: String, var type: Type? = null,
									val value: Value? = null, val isConstant: Boolean = true,
									val isMutable: Boolean = false): Unit(source) {

	init {
		type?.let { type ->
			units.add(type)
		}
		if(value != null)
			units.add(value)
	}

	constructor(source: Identifier): this(source, source.getValue(), null, null, true, true)

	open fun withTypeSubstitutions(typeSubstitution: Map<ObjectType, Type>): VariableValueDeclaration {
		return VariableValueDeclaration(source, name, type?.withTypeSubstitutions(typeSubstitution), value, isConstant,
			isMutable)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		if(value == null) {
			if(type == null)
				linter.addMessage(source, "Type or value is required.", Message.Type.ERROR)
		} else {
			if(value.isAssignableTo(type)) {
				value.setInferredType(type)
			} else if(type == null) {
				type = value.type
			} else {
				linter.addMessage(source, "Type '${value.type}' is not assignable to type '$type'.",
					Message.Type.ERROR)
			}
		}
	}
}
