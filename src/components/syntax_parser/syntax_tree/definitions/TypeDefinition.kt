package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.Class
import components.semantic_analysis.semantic_model.definitions.Enum
import components.semantic_analysis.semantic_model.definitions.Object
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSection
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSectionChild
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.literals.Identifier
import components.syntax_parser.syntax_tree.literals.ObjectType
import components.tokenizer.Word
import components.tokenizer.WordAtom
import errors.internal.CompilerError
import messages.Message
import components.semantic_analysis.semantic_model.definitions.TypeDefinition as SemanticTypeDefinitionModel

class TypeDefinition(private val identifier: Identifier, private val type: Word, private val explicitParentType: ObjectType?,
					 private val superType: TypeElement?, private val body: TypeBody?):
	Element(type.start, (body ?: superType ?: type).end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	override fun concretize(linter: Linter, scope: MutableScope): SemanticTypeDefinitionModel {
		val name = identifier.getValue()
		val explicitParentType = explicitParentType?.concretize(linter, scope)
		var superType = superType?.concretize(linter, scope)
		val typeDefinition = when(type.type) {
			WordAtom.CLASS -> {
				parent?.validate(linter, Class.ALLOWED_MODIFIER_TYPES)
				val isAbstract = parent?.containsModifier(WordAtom.ABSTRACT) ?: false
				val isBound = parent?.containsModifier(WordAtom.BOUND) ?: false
				val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
				val isMutable = !(parent?.containsModifier(WordAtom.IMMUTABLE) ?: false)
				if(!Linter.SpecialType.isRootType(name))
					superType = superType ?: LiteralType(this, Linter.SpecialType.ANY)
				val typeScope = TypeScope(scope, superType?.scope)
				Class(this, name, typeScope, explicitParentType, superType, isAbstract, isBound, isNative, isMutable)
			}
			WordAtom.OBJECT -> {
				parent?.validate(linter, Object.ALLOWED_MODIFIER_TYPES)
				val isBound = parent?.containsModifier(WordAtom.BOUND) ?: false
				val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
				val isMutable = !(parent?.containsModifier(WordAtom.IMMUTABLE) ?: false)
				superType = superType ?: LiteralType(this, Linter.SpecialType.ANY)
				val typeScope = TypeScope(scope, superType.scope)
				Object(this, name, typeScope, explicitParentType, superType, isBound, isNative, isMutable)
			}
			WordAtom.ENUM -> {
				parent?.validate(linter, Enum.ALLOWED_MODIFIER_TYPES)
				val isBound = parent?.containsModifier(WordAtom.BOUND) ?: false
				superType = superType ?: LiteralType(this, Linter.SpecialType.ANY)
				val typeScope = TypeScope(scope, superType.scope)
				Enum(this, name, typeScope, explicitParentType, superType, isBound)
			}
			else -> throw CompilerError("Encountered unknown type definition type.")
		}
		var instanceList: InstanceList? = null
		if(body != null) {
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
				member.concretize(linter, typeDefinition.scope, typeDefinition.units)
			}
		}
		for(unit in typeDefinition.units)
			unit.parent = typeDefinition
		typeDefinition.register(linter, scope)
		return typeDefinition
	}

	override fun toString(): String {
		var stringRepresentation = "TypeDefinition [ $identifier ${type.getValue()}"
		if(explicitParentType != null)
			stringRepresentation += " in $explicitParentType"
		if(superType != null)
			stringRepresentation += ": $superType"
		stringRepresentation += " ]"
		if(body != null)
			stringRepresentation += " { $body }"
		return stringRepresentation
	}
}
