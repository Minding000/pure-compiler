package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.MemberDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.modifiers.MissingOverridingKeyword
import logger.issues.modifiers.OverriddenSuperMissing

abstract class InterfaceMember(source: Element, scope: Scope, name: String, type: Type? = null, value: Value? = null,
							   val isStatic: Boolean = false, override val isAbstract: Boolean = false, isConstant: Boolean = true,
							   isMutable: Boolean = false, val isOverriding: Boolean = false):
	ValueDeclaration(source, scope, name, type, value, isConstant, isMutable), MemberDeclaration {
	override lateinit var parentDefinition: TypeDefinition
	override val memberIdentifier
		get() = "$name${if(type == null) "" else ": $type"}"
	var superMember: InterfaceMember? = null

	abstract override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): InterfaceMember

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(value !is Function) {
			if(superMember == null) {
				if(isOverriding)
					linter.addIssue(OverriddenSuperMissing(source, "property"))
			} else {
				if(!isOverriding)
					linter.addIssue(MissingOverridingKeyword(source, "Property", memberIdentifier))
			}
		}
	}
}
