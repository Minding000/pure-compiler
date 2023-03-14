package logger.issues.resolution

import components.semantic_analysis.semantic_model.general.Unit
import components.syntax_parser.syntax_tree.general.Element
import logger.Issue
import logger.Severity

class SignatureAmbiguity(source: Element, kind: String, signature: String, matchingSignatures: List<Unit>):
	Issue(Severity.ERROR, source) {
	override val text = "Call to $kind '$signature' is ambiguous. Matching signatures:" +
		matchingSignatures.joinToString("") { "\n - '$it' declared at ${it.source.getStartString()}" }
	override val description =
		"The callable exists, but there are multiple overloads with parameters that accept the provided types and values."
}
