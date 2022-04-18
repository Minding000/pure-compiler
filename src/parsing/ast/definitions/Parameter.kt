package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.Parameter
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type

class Parameter(private val modifierList: ModifierList?, private val identifier: Identifier, private val type: Type?):
    Element(modifierList?.start ?: identifier.start, identifier.end) {

    override fun concretize(linter: Linter, scope: Scope): Unit {
        //TODO include modifiers
        val parameter = Parameter(this, identifier.getValue(), type?.concretize(linter, scope))
        if(type != null)
            scope.declareValue(parameter)
        return parameter
    }

    override fun toString(): String {
        return "Parameter${if(modifierList == null) "" else " [ $modifierList ]"} { $identifier${if(type == null) "" else ": $type"} }"
    }
}