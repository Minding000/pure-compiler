package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.*
import components.semantic_analysis.semantic_model.definitions.Enum
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.semantic_analysis.semantic_model.values.Function
import components.semantic_analysis.semantic_model.values.Operator
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSection
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSectionChild
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.literals.Identifier
import components.syntax_parser.syntax_tree.literals.ObjectType
import components.tokenizer.Word
import components.tokenizer.WordAtom
import errors.internal.CompilerError
import logger.issues.definition.GenericTypeDeclarationInObject
import logger.issues.definition.InvalidInstanceLocation
import logger.issues.definition.MultipleInstanceLists
import java.util.*
import components.semantic_analysis.semantic_model.definitions.TypeDefinition as SemanticTypeDefinitionModel

class TypeDefinition(private val identifier: Identifier, private val type: Word, private val explicitParentType: ObjectType?,
					 private val superType: TypeElement?, private val body: TypeBody?):
	Element(type.start, (body ?: superType ?: type).end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_CLASS_MODIFIERS = listOf(WordAtom.ABSTRACT, WordAtom.BOUND, WordAtom.IMMUTABLE, WordAtom.NATIVE)
		val ALLOWED_OBJECT_MODIFIERS = listOf(WordAtom.BOUND, WordAtom.NATIVE, WordAtom.IMMUTABLE)
		val ALLOWED_ENUM_MODIFIERS = listOf(WordAtom.BOUND)
	}

	override fun concretize(linter: Linter, scope: MutableScope): SemanticTypeDefinitionModel {
		val name = identifier.getValue()
		val definitionType = type.type
		var superType = superType?.concretize(linter, scope)
		if(!(definitionType == WordAtom.CLASS && Linter.SpecialType.isRootType(name)))
			superType = superType ?: LiteralType(this, scope, Linter.SpecialType.ANY)
		val typeScope = TypeScope(scope, superType?.interfaceScope)
		val explicitParentType = explicitParentType?.concretize(linter, scope)
		val members = concretizeMembers(linter, typeScope, definitionType).toMutableList()
		val isBound = parent?.containsModifier(WordAtom.BOUND) ?: false
		val typeDefinition = when(definitionType) {
			WordAtom.CLASS -> {
				val isAbstract = parent?.containsModifier(WordAtom.ABSTRACT) ?: false
				val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
				val isMutable = !(parent?.containsModifier(WordAtom.IMMUTABLE) ?: false)
				parent?.validate(linter, ALLOWED_CLASS_MODIFIERS)
				Class(this, name, typeScope, explicitParentType, superType, members, isAbstract, isBound, isNative, isMutable)
			}
			WordAtom.OBJECT -> {
				val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
				val isMutable = !(parent?.containsModifier(WordAtom.IMMUTABLE) ?: false)
				parent?.validate(linter, ALLOWED_OBJECT_MODIFIERS)
				Object(this, name, typeScope, explicitParentType, superType, members, isBound, isNative, isMutable)
			}
			WordAtom.ENUM -> {
				parent?.validate(linter, ALLOWED_ENUM_MODIFIERS)
				Enum(this, name, typeScope, explicitParentType, superType, members, isBound)
			}
			else -> throw CompilerError(this, "Encountered unknown type definition type: $definitionType")
		}
		return typeDefinition
	}

	private fun concretizeMembers(linter: Linter, typeScope: TypeScope, definitionType: WordAtom): List<Unit> {
		val explicitMembers = concretizeExplicitlyDeclaredMembers(linter, typeScope, definitionType)
		val members = LinkedList<Unit>()
		val functions = HashMap<String, Function>()
		val operators = HashMap<Operator.Kind, Operator>()
		for(member in explicitMembers) {
			if(member is FunctionImplementation) {
				val source = member.source
				if(source is FunctionDefinition) {
					//TODO add additional static instance access property
					// - e.g. "cars.sortBy(Car::range)"
					// - not for object definitions
					// - same for operators
					val name = source.getName()
					var function = functions[name]
					if(function == null) {
						function = Function(source, typeScope, name)
						functions[name] = function
						members.add(PropertyDeclaration(source, typeScope, function.name, function.type, function, false,
							function.isAbstract))
					}
					function.addImplementation(member)
				} else if(source is OperatorDefinition) {
					val kind = source.getKind()
					var operator = operators[kind]
					if(operator == null) {
						operator = Operator(source, typeScope, kind)
						operators[kind] = operator
						members.add(PropertyDeclaration(operator.source, typeScope, operator.name, operator.type, operator, false,
							operator.isAbstract))
					}
					operator.addImplementation(member)
				}
				continue
			}
			members.add(member)
		}
		return members
	}

	private fun concretizeExplicitlyDeclaredMembers(linter: Linter, typeScope: TypeScope, definitionType: WordAtom): List<Unit> {
		if(body == null)
			return emptyList()
		var instanceList: InstanceList? = null
		val members = LinkedList<Unit>()
		for(member in body.members) {
			if(member is InstanceList) {
				if(!(definitionType == WordAtom.CLASS || definitionType == WordAtom.ENUM)) {
					linter.addIssue(InvalidInstanceLocation(member))
					continue
				}
				if(instanceList == null) {
					instanceList = member
				} else {
					linter.addIssue(MultipleInstanceLists(member))
				}
			} else if(member is GenericsDeclaration) {
				if(definitionType == WordAtom.OBJECT) {
					linter.addIssue(GenericTypeDeclarationInObject(member))
					continue
				}
			}
			member.concretize(linter, typeScope, members)
		}
		return members
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
