package logger.issues.resolution

import components.semantic_analysis.semantic_model.declarations.FunctionSignature
import components.semantic_analysis.semantic_model.general.SemanticModel
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class SignatureAmbiguity(source: SyntaxTreeNode, kind: String, signature: String, matchingSignatures: List<SemanticModel>):
	Issue(Severity.ERROR, source) {
	override val text = "Call to $kind '$signature' is ambiguous. Matching signatures:" +
		matchingSignatures.joinToString("") { signature ->
			"\n - '${if(signature is FunctionSignature) signature.original else signature}'" +
				" declared at ${signature.source.getStartString()}" }
	override val description =
		"The callable exists, but there are multiple overloads with parameters that accept the provided types and values."
}
