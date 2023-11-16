package logger.issues.declaration

import components.semantic_model.declarations.MemberDeclaration
import components.semantic_model.declarations.TypeDeclaration
import logger.Issue
import logger.Severity

class MissingSpecificOverrides(val typeDeclaration: TypeDeclaration, val missingOverrides: Map<TypeDeclaration, List<MemberDeclaration>>):
	Issue(Severity.ERROR, typeDeclaration.source) {
	override val text: String
		get() {
			var stringRepresentation = "Type declaration '${typeDeclaration.name}' does not override the following specific members:"
			for((parent, missingMembers) in missingOverrides) {
				stringRepresentation += "\n - ${parent.name}"
				for(member in missingMembers)
					stringRepresentation += "\n   - ${member.memberIdentifier}"
			}
			return stringRepresentation
		}
	override val description = "Specific members need to be overridden in every sub-class."
	override val suggestion = "Override specific members."
}
