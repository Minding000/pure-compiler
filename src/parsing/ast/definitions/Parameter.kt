package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.Parameter
import linter.elements.general.Unit
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.literals.Identifier

class Parameter(private val modifierList: ModifierList?, private val identifier: Element):
    Element(modifierList?.start ?: identifier.start, identifier.end) {

    override fun concretize(linter: Linter, scope: Scope): Unit {
        //TODO include modifiers
        var name = "<unknown>"
        var type: Unit? = null
        when(identifier) {
            is Identifier -> name = identifier.getValue()
            is TypedIdentifier -> {
                name = identifier.identifier.getValue()
                type = identifier.type.concretize(linter, scope)
            }
            else -> linter.messages.add(Message("Parameter has unknown identifier type."))
        }
        val parameter = Parameter(this, name, type)
        if(type != null)
            scope.declareValue(parameter)
        return parameter
    }

    override fun toString(): String {
        return "Parameter${if(modifierList == null) "" else " [ $modifierList ]"} { $identifier }"
    }
}