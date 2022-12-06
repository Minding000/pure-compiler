package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element

abstract class MemberDeclaration(source: Element, name: String, type: Type? = null, value: Value? = null,
								 val isAbstract: Boolean = false, isConstant: Boolean = true,
								 isMutable: Boolean = false):
	VariableValueDeclaration(source, name, type, value, isConstant, isMutable) {
	//val parentDefinition: TypeDefinition
	//val signature: String

	abstract override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): MemberDeclaration
}
