package components.parsing.syntax_tree.definitions

import errors.internal.CompilerError
import linting.Linter
import linting.semantic_model.definitions.*
import linting.semantic_model.definitions.Enum
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.scopes.TypeScope
import messages.Message
import components.parsing.syntax_tree.definitions.sections.ModifierSection
import components.parsing.syntax_tree.definitions.sections.ModifierSectionChild
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.TypeElement
import components.parsing.syntax_tree.literals.Identifier
import components.tokenizer.Word
import components.tokenizer.WordAtom
import linting.semantic_model.definitions.TypeDefinition as SemanticTypeDefinitionModel

class TypeDefinition(private val type: Word, private val identifier: Identifier, private val superType: TypeElement?,
					 private val body: TypeBody):
	Element(type.start, body.end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	override fun concretize(linter: Linter, scope: MutableScope): SemanticTypeDefinitionModel {
		val name = identifier.getValue()
		val superType = superType?.concretize(linter, scope)
		val typeScope = TypeScope(scope, superType?.scope)
		val typeDefinition = when(type.type) {
			WordAtom.CLASS -> {
				parent?.validate(linter, Class.ALLOWED_MODIFIER_TYPES)
				val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
				val isMutable = !(parent?.containsModifier(WordAtom.IMMUTABLE) ?: false)
				Class(this, name, typeScope, superType, isNative, isMutable)
			}
			WordAtom.OBJECT -> {
				parent?.validate(linter, Object.ALLOWED_MODIFIER_TYPES)
				val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
				val isMutable = !(parent?.containsModifier(WordAtom.IMMUTABLE) ?: false)
				Object(this, name, typeScope, superType, isNative, isMutable)
			}
			WordAtom.ENUM -> {
				parent?.validate(linter)
				Enum(this, name, typeScope, superType)
			}
			WordAtom.TRAIT -> {
				parent?.validate(linter)
				Trait(this, name, typeScope, superType)
			}
			else -> throw CompilerError("Encountered unknown type type.")
		}
		var instanceList: InstanceList? = null
		for(member in body.members) {
			if(member is InstanceList) {
				if(!(typeDefinition is Enum || typeDefinition is Class)) {
					linter.addMessage(member, "Instance declarations are only allowed in enums and classes.",
						Message.Type.WARNING)
					continue
				}
				if(instanceList == null) {
					instanceList = member
				} else {
					linter.addMessage(member, "Instance declarations can be merged.", Message.Type.WARNING)
				}
			} else if(member is GenericsDeclaration) {
				if(typeDefinition is Object) {
					linter.addMessage(member, "Generic type declarations are not allowed in objects.",
						Message.Type.WARNING)
					continue
				}
			}
			member.concretize(linter, typeScope, typeDefinition.units)
		}
		typeDefinition.register(linter, scope)
		return typeDefinition
	}

	override fun toString(): String {
		return "TypeDefinition [ ${type.getValue()} $identifier${if(superType == null) "" else " $superType"} ] { $body }"
	}
}
