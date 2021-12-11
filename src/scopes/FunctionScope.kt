package scopes

import elements.identifier.ClassIdentifier
import elements.identifier.Identifier
import errors.user.SyntaxError

class FunctionScope(parentScope: ClassScope): SubScope(parentScope) {

	override fun declareIdentifier(identifier: Identifier) {
		if(identifier is ClassIdentifier)
			throw SyntaxError("Can't declare a class inside of a function.")
		super.declareIdentifier(identifier)
	}
}