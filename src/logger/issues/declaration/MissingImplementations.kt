package logger.issues.declaration

import components.semantic_model.declarations.MemberDeclaration
import components.semantic_model.declarations.TypeDeclaration
import logger.Issue
import logger.Severity
import java.util.*

class MissingImplementations(val typeDeclaration: TypeDeclaration, val missingOverrides: Map<TypeDeclaration, LinkedList<MemberDeclaration>>):
	Issue(Severity.ERROR, typeDeclaration.source) {
	override val text: String
		get() {
			var stringRepresentation = "Non-abstract type declaration '${typeDeclaration.name}' does not implement the following inherited members:"
			for((parent, missingMembers) in missingOverrides) {
				stringRepresentation += "\n - ${parent.name}"
				for(member in missingMembers)
					stringRepresentation += "\n   - ${member.memberIdentifier}"
			}
			return stringRepresentation
		}
	override val description = "Implementations of abstract classes have to implement all inherited unimplemented abstract members."
	override val suggestion = "Implement unimplemented abstract members."
}
