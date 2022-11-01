package errors.user

import components.linting.semantic_model.general.Unit

class SignatureResolutionAmbiguityError(val signatures: List<Unit>): Error()
