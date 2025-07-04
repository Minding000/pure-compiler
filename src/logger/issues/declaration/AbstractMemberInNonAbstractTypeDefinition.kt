package logger.issues.declaration

import components.semantic_model.declarations.MemberDeclaration
import components.semantic_model.declarations.TypeDeclaration
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.AddModifier

class AbstractMemberInNonAbstractTypeDefinition(memberDeclaration: MemberDeclaration, typeDeclaration: TypeDeclaration):
	Issue(Severity.ERROR, memberDeclaration.source) {
	override val text = "Abstract member '${memberDeclaration.memberIdentifier}' is not allowed" +
		" in non-abstract type declaration '${typeDeclaration.name}'."
	override val description = "Only abstract classes can contain abstract members."
	override val suggestion = "Add 'abstract' modifier."
	override val suggestedAction = AddModifier(WordAtom.ABSTRACT)
}
