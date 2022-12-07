package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element

abstract class MemberDeclaration(source: Element, name: String, type: Type? = null, value: Value? = null,
								 val isAbstract: Boolean = false, isConstant: Boolean = true,
								 isMutable: Boolean = false):
	ValueDeclaration(source, name, type, value, isConstant, isMutable) {
	lateinit var parentDefinition: TypeDefinition

	abstract override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): MemberDeclaration

	fun canBeOverriddenBy(other: MemberDeclaration?): Boolean {
		if(other == null)
			return false
		if(other.name !== name)
			return false
		return true
	}
}
