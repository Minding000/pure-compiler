package parsing.ast.definitions

import errors.internal.CompilerError
import linter.Linter
import linter.elements.definitions.Class
import linter.elements.definitions.Enum
import linter.elements.definitions.Object
import linter.elements.definitions.Trait
import linter.elements.general.Unit
import linter.scopes.Scope
import linter.scopes.TypeScope
import parsing.ast.definitions.sections.ModifierSection
import parsing.ast.definitions.sections.ModifierSectionChild
import parsing.ast.general.Element
import parsing.ast.literals.Identifier
import parsing.ast.general.TypeElement
import parsing.tokenizer.Word
import parsing.tokenizer.WordAtom

class TypeDefinition(private val modifierList: ModifierList?, private val type: Word,
					 private val identifier: Identifier, private val superType: TypeElement?,
					 private val body: TypeBody):
	Element(modifierList?.start ?: type.start, body.end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.NATIVE)
	}

	override fun concretize(linter: Linter, scope: Scope): Unit {
		val name = identifier.getValue()
		val superType = superType?.concretize(linter, scope)
		val typeScope = TypeScope(scope, superType?.scope)
		val typeDefinition = when(type.type) {
			WordAtom.CLASS -> {
				modifierList?.validate(linter, ALLOWED_MODIFIER_TYPES)
				val isNative = modifierList?.contains(WordAtom.NATIVE) ?: false
				val clazz = Class(this, name, typeScope, superType, isNative)
				scope.declareType(linter, clazz)
				clazz
			}
			WordAtom.OBJECT -> {
				modifierList?.validate(linter)
				val obj = Object(this, name, typeScope, superType)
				scope.declareType(linter, obj)
				scope.declareValue(linter, obj.value)
				obj
			}
			WordAtom.ENUM -> {
				modifierList?.validate(linter)
				val enum = Enum(this, name, typeScope, superType)
				scope.declareType(linter, enum)
				scope.declareValue(linter, enum.value)
				enum
			}
			WordAtom.TRAIT -> {
				modifierList?.validate(linter)
				val trait = Trait(this, name, typeScope, superType)
				scope.declareType(linter, trait)
				trait
			}
			else -> throw CompilerError("Encountered unknown type type.")
		}
		for(member in body.members)
			member.concretize(linter, typeScope, typeDefinition.units)
		return typeDefinition
	}

	override fun toString(): String {
		return "TypeDefinition [ ${if(modifierList == null) "" else "$modifierList "}${type.getValue()} $identifier${if(superType == null) "" else " $superType"} ] { $body }"
	}
}