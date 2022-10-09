package errors.user

import linting.semantic_model.general.Unit

class SignatureResolutionAmbiguityError(val signatures: List<Unit>): Error()
