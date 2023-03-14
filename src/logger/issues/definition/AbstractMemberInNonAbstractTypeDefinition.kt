package logger.issues.definition

import components.semantic_analysis.semantic_model.definitions.MemberDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.AddModifier

class AbstractMemberInNonAbstractTypeDefinition(memberDeclaration: MemberDeclaration, typeDefinition: TypeDefinition):
	Issue(Severity.ERROR, memberDeclaration.source) {
	override val text = "Abstract member '${memberDeclaration.memberIdentifier}' is not allowed" +
		" in non-abstract type definition '${typeDefinition.name}'."
	override val description = "Only abstract classes can contain abstract members."
	override val suggestion = "Add 'abstract' modifier."
	override val suggestedAction = AddModifier(WordAtom.ABSTRACT)
}
