package components.semantic_model.values

import components.semantic_model.declarations.MemberDeclaration
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.modifiers.MissingOverridingKeyword
import logger.issues.modifiers.OverriddenSuperMissing

//TODO Name and purpose is unclear.
// Only Property and Instance inherit from InterfaceMember
abstract class InterfaceMember(source: SyntaxTreeNode, scope: MutableScope, name: String, type: Type? = null, value: Value? = null,
							   val isStatic: Boolean = false, override val isAbstract: Boolean = false, isConstant: Boolean = true,
							   isMutable: Boolean = false, val isOverriding: Boolean = false):
	ValueDeclaration(source, scope, name, type, value, isConstant, isMutable), MemberDeclaration {
	override lateinit var parentTypeDeclaration: TypeDeclaration
	override val memberIdentifier
		get() = "$name${if(type == null) "" else ": $type"}"
	var superMember: Pair<InterfaceMember, Type?>? = null

	override fun validate() {
		super.validate()
		if(value !is Function) {
			if(superMember == null) {
				if(isOverriding)
					context.addIssue(OverriddenSuperMissing(source, "property"))
			} else {
				if(!isOverriding)
					context.addIssue(MissingOverridingKeyword(source, "Property", memberIdentifier))
			}
		}
	}
}
