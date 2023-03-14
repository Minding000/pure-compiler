package logger.issues.definition

import components.semantic_analysis.semantic_model.definitions.MemberDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import logger.Issue
import logger.Severity
import java.util.*

class MissingImplementations(val typeDefinition: TypeDefinition, val missingOverrides: Map<TypeDefinition, LinkedList<MemberDeclaration>>):
	Issue(Severity.ERROR, typeDefinition.source) {
	override val text: String
		get() {
			var stringRepresentation = "Non-abstract class '${typeDefinition.name}' does not implement the following inherited members:"
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
