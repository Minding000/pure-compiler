package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.*
import components.semantic_model.declarations.Enum
import components.semantic_model.declarations.PropertyDeclaration
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.MutableScope
import components.semantic_model.scopes.TypeScope
import components.semantic_model.types.LiteralType
import components.semantic_model.values.Function
import components.semantic_model.values.Operator
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSection
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSectionChild
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import components.syntax_parser.syntax_tree.literals.ObjectType
import components.tokenizer.Word
import components.tokenizer.WordAtom
import errors.internal.CompilerError
import logger.issues.declaration.GenericTypeDeclarationInObject
import logger.issues.declaration.InvalidInstanceLocation
import logger.issues.declaration.MultipleInstanceLists
import java.util.*
import components.semantic_model.declarations.TypeDeclaration as SemanticTypeDeclarationModel

class TypeDefinition(private val identifier: Identifier, private val type: Word, private val explicitParentType: ObjectType?,
					 private val superType: TypeSyntaxTreeNode?, private val body: TypeBody?):
	SyntaxTreeNode(type.start, (body ?: superType ?: type).end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_CLASS_MODIFIERS = listOf(WordAtom.ABSTRACT, WordAtom.BOUND, WordAtom.COPIED, WordAtom.IMMUTABLE, WordAtom.NATIVE)
		val ALLOWED_OBJECT_MODIFIERS = listOf(WordAtom.BOUND, WordAtom.NATIVE, WordAtom.IMMUTABLE)
		val ALLOWED_ENUM_MODIFIERS = listOf(WordAtom.BOUND)
	}

	override fun toSemanticModel(scope: MutableScope, semanticModels: MutableList<SemanticModel>) {
		val typeDefinition = toSemanticModel(scope)
		val valueDeclaration = typeDefinition.getValueDeclaration()
		if(valueDeclaration != null)
			semanticModels.add(valueDeclaration)
		semanticModels.add(typeDefinition)
	}

	override fun toSemanticModel(scope: MutableScope): SemanticTypeDeclarationModel {
		val name = identifier.getValue()
		val definitionType = type.type
		val typeScope = TypeScope(scope)
		val superType = superType?.toSemanticModel(typeScope) ?: if(definitionType == WordAtom.CLASS) {
			if(SpecialType.isRootType(name)) {
				null
			} else {
				if((parent?.containsModifier(WordAtom.ABSTRACT) == true) || (parent?.containsModifier(WordAtom.COPIED) == true)) {
					LiteralType(this, typeScope, SpecialType.ANY)
				} else {
					LiteralType(this, typeScope, SpecialType.IDENTIFIABLE)
				}
			}
		} else {
			LiteralType(this, typeScope, SpecialType.IDENTIFIABLE)
		}
		typeScope.superScope = superType?.interfaceScope
		val explicitParentType = explicitParentType?.toSemanticObjectTypeModel(scope)
		val members = getSemanticMemberModels(typeScope, definitionType).toMutableList()
		val isBound = parent?.containsModifier(WordAtom.BOUND) ?: false
		val typeDefinition = when(definitionType) {
			WordAtom.CLASS -> {
				val isAbstract = parent?.containsModifier(WordAtom.ABSTRACT) ?: false
				val isCopied = parent?.containsModifier(WordAtom.COPIED) ?: false
				val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
				val isMutable = !(parent?.containsModifier(WordAtom.IMMUTABLE) ?: false)
				parent?.validate(ALLOWED_CLASS_MODIFIERS)
				Class(this, name, typeScope, explicitParentType, superType, members, isAbstract, isBound, isCopied, isNative,
					isMutable)
			}
			WordAtom.OBJECT -> {
				val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
				val isMutable = !(parent?.containsModifier(WordAtom.IMMUTABLE) ?: false)
				parent?.validate(ALLOWED_OBJECT_MODIFIERS)
				Object(this, name, typeScope, explicitParentType, superType, members, isBound, isNative, isMutable)
			}
			WordAtom.ENUM -> {
				parent?.validate(ALLOWED_ENUM_MODIFIERS)
				Enum(this, name, typeScope, explicitParentType, superType, members, isBound)
			}
			else -> throw CompilerError(this, "Encountered unknown type definition type: $definitionType")
		}
		return typeDefinition
	}

	private fun getSemanticMemberModels(typeScope: TypeScope, definitionType: WordAtom): List<SemanticModel> {
		val explicitMembers = getSemanticModelsOfExplicitlyDeclaredMembers(typeScope, definitionType)
		val members = LinkedList<SemanticModel>()
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
						val property = PropertyDeclaration(source, typeScope, function.name, null, function, false,
							function.isAbstract)
						property.type = function.type
						members.add(property)
					}
					function.addImplementation(member)
				} else if(source is OperatorDefinition) {
					val kind = source.getKind()
					var operator = operators[kind]
					if(operator == null) {
						operator = Operator(source, typeScope, kind)
						operators[kind] = operator
						val property = PropertyDeclaration(operator.source, typeScope, operator.name, null, operator, false,
							operator.isAbstract)
						property.type = operator.type
						members.add(property)
					}
					operator.addImplementation(member)
				}
				continue
			}
			members.add(member)
		}
		return members
	}

	private fun getSemanticModelsOfExplicitlyDeclaredMembers(typeScope: TypeScope, definitionType: WordAtom): List<SemanticModel> {
		if(body == null)
			return emptyList()
		var instanceList: InstanceList? = null
		val members = LinkedList<SemanticModel>()
		for(member in body.members) {
			if(member is InstanceList) {
				if(!(definitionType == WordAtom.CLASS || definitionType == WordAtom.ENUM)) {
					context.addIssue(InvalidInstanceLocation(member))
					continue
				}
				if(instanceList == null) {
					instanceList = member
				} else {
					context.addIssue(MultipleInstanceLists(member))
				}
			} else if(member is GenericsDeclaration) {
				if(definitionType == WordAtom.OBJECT) {
					context.addIssue(GenericTypeDeclarationInObject(member))
					continue
				}
			}
			member.toSemanticModel(typeScope, members)
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
