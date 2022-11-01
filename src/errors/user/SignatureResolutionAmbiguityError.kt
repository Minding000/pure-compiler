package errors.user

import components.semantic_analysis.semantic_model.general.Unit

class SignatureResolutionAmbiguityError(val signatures: List<Unit>): Error()
