package errors.user

import components.semantic_model.general.SemanticModel
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.resolution.SignatureAmbiguity

class SignatureResolutionAmbiguityError(private val signatures: List<SemanticModel>): Error() {

	fun log(source: SyntaxTreeNode, subject: String, signature: String) {
		source.context.addIssue(SignatureAmbiguity(source, subject, signature, signatures))
	}
}
