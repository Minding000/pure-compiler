package errors.user

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.resolution.SignatureAmbiguity

class SignatureResolutionAmbiguityError(private val signatures: List<Unit>): Error() {

	fun log(linter: Linter, source: Element, subject: String, signature: String) {
		linter.addIssue(SignatureAmbiguity(source, subject, signature, signatures))
	}
}
