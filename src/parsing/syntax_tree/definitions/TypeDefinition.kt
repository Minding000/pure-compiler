package parsing.syntax_tree.definitions

import errors.internal.CompilerError
import linting.Linter
import messages.Message
import linting.semantic_model.definitions.Class
import linting.semantic_model.definitions.Enum
import linting.semantic_model.definitions.Object
import linting.semantic_model.definitions.Trait
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.scopes.TypeScope
import parsing.syntax_tree.definitions.sections.ModifierSection
import parsing.syntax_tree.definitions.sections.ModifierSectionChild
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.literals.Identifier
import parsing.syntax_tree.general.TypeElement
import parsing.tokenizer.Word
import parsing.tokenizer.WordAtom

class TypeDefinition(private val type: Word, private val identifier: Identifier, private val superType: TypeElement?,
					 private val body: TypeBody):
	Element(type.start, body.end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	override fun concretize(linter: Linter, scope: MutableScope): Unit {
		val name = identifier.getValue()
		val superType = superType?.concretize(linter, scope)
		val typeScope = TypeScope(scope, superType?.scope)
		val typeDefinition = when(type.type) {
			WordAtom.CLASS -> {
				parent?.validate(linter, Class.ALLOWED_MODIFIER_TYPES)
				val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
				val clazz = Class(this, name, typeScope, superType, isNative)
				typeScope.typeDefinition = clazz
				scope.declareType(linter, clazz)
				scope.declareValue(linter, clazz.value)
				clazz
			}
			WordAtom.OBJECT -> {
				parent?.validate(linter, Object.ALLOWED_MODIFIER_TYPES)
				val obj = Object(this, name, typeScope, superType)
				typeScope.typeDefinition = obj
				scope.declareType(linter, obj)
				scope.declareValue(linter, obj.value)
				obj
			}
			WordAtom.ENUM -> {
				parent?.validate(linter)
				val enum = Enum(this, name, typeScope, superType)
				typeScope.typeDefinition = enum
				scope.declareType(linter, enum)
				scope.declareValue(linter, enum.value)
				enum
			}
			WordAtom.TRAIT -> {
				parent?.validate(linter)
				val trait = Trait(this, name, typeScope, superType)
				typeScope.typeDefinition = trait
				scope.declareType(linter, trait)
				trait
			}
			else -> throw CompilerError("Encountered unknown type type.")
		}
		for(member in body.members) {
			if(typeDefinition is Object || typeDefinition is Trait) {
				if(member is Instance) {
					linter.addMessage(member, "Instance declarations are not allowed in objects and traits.",
						Message.Type.WARNING)
					continue
				}
			}
			member.concretize(linter, typeScope, typeDefinition.units)
		}
		return typeDefinition
	}

	override fun toString(): String {
		return "TypeDefinition [ ${type.getValue()} $identifier${if(superType == null) "" else " $superType"} ] { $body }"
	}
}