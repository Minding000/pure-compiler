package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.semantic_model.definitions.MemberDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element

abstract class InterfaceMember(source: Element, name: String, type: Type? = null, value: Value? = null,
							   override val isAbstract: Boolean = false, isConstant: Boolean = true,
							   isMutable: Boolean = false):
	ValueDeclaration(source, name, type, value, isConstant, isMutable), MemberDeclaration {
	override lateinit var parentDefinition: TypeDefinition
	override var memberIdentifier = "$name${if(type == null) "" else ": $type"}"

	abstract override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): InterfaceMember
}
