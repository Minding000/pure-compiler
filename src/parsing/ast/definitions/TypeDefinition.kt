package parsing.ast.definitions

import errors.internal.CompilerError
import linter.Linter
import linter.elements.definitions.Class
import linter.elements.definitions.Enum
import linter.elements.definitions.Object
import linter.elements.definitions.Trait
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.general.MetaElement
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type
import parsing.tokenizer.Word
import parsing.tokenizer.WordAtom
import java.util.*

class TypeDefinition(private val modifierList: ModifierList?, private val type: Word,
					 private val identifier: Identifier, private val superType: Type?,
					 private val body: TypeBody): Element(modifierList?.start ?: type.start, body.end) {

	override fun concretize(linter: Linter, scope: Scope): Unit {
		//TODO include modifiers
		val name = identifier.getValue()
		val superType = superType?.concretize(linter, scope)
		val typeDefinition = when(type.type) {
			WordAtom.CLASS -> Class(this, name, superType)
			WordAtom.OBJECT -> Object(this, name, superType)
			WordAtom.ENUM -> Enum(this, name, superType)
			WordAtom.TRAIT -> Trait(this, name, superType)
			else -> throw CompilerError("Encountered unknown type type.")
		}
		for(member in body.members)
			if(member !is MetaElement) //TODO include generic declarations
				typeDefinition.units.add(member.concretize(linter, scope))
		return typeDefinition
	}

	override fun toString(): String {
		return "TypeDefinition [ ${if(modifierList == null) "" else "$modifierList "}${type.getValue()} $identifier${if(superType == null) "" else " $superType"} ] { $body }"
	}
}