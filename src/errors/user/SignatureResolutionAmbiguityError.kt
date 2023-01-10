package errors.user

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.syntax_parser.syntax_tree.general.Element
import messages.Message

class SignatureResolutionAmbiguityError(val signatures: List<Unit>): Error() {

	fun log(linter: Linter, source: Element, subject: String, signature: String) {
		linter.addMessage(source, "Call to $subject '$signature' is ambiguous. Matching signatures:" + getSignatureList(),
			Message.Type.ERROR)
	}

	fun getSignatureList(): String {
		var signatureList = ""
		for(signature in signatures) {
			signatureList += "\n - '$signature' declared at ${signature.source.getStartString()}"
		}
		return signatureList
	}
}
