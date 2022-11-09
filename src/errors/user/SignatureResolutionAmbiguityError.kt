package errors.user

import components.semantic_analysis.semantic_model.general.Unit

class SignatureResolutionAmbiguityError(val signatures: List<Unit>): Error() {

	fun getSignatureList(): String {
		var signatureList = ""
		for(signature in signatures) {
			signatureList += "\n - '$signature' declared at ${signature.source.getStartString()}"
		}
		return signatureList
	}
}
