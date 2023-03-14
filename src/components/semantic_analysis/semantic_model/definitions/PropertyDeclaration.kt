package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.initialization.CircularAssignment

class PropertyDeclaration(source: Element, scope: Scope, name: String, type: Type? = null, value: Value? = null, isStatic: Boolean = false,
						  isAbstract: Boolean = false, isConstant: Boolean = true, isMutable: Boolean = false,
						  isOverriding: Boolean = false):
	InterfaceMember(source, scope, name, type, value, isStatic, isAbstract, isConstant, isMutable, isOverriding) {

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): PropertyDeclaration {
		return PropertyDeclaration(source, scope, name, type?.withTypeSubstitutions(typeSubstitutions), value, isStatic, isAbstract,
			isConstant, isMutable, isOverriding)
	}

	override fun linkValues(linter: Linter) {
		if(linter.propertyDeclarationStack.contains(this)) {
			for(propertyDeclaration in linter.propertyDeclarationStack)
				linter.addIssue(CircularAssignment(propertyDeclaration))
			return
		}
		linter.propertyDeclarationStack.add(this)
		super.linkValues(linter)
		linter.propertyDeclarationStack.remove(this)
	}
}
