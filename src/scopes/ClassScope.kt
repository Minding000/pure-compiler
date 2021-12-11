package scopes

import elements.identifier.Identifier
import elements.identifier.ClassIdentifier
import errors.user.SyntaxError

class ClassScope(parentScope: GlobalScope): SubScope(parentScope) {

	override fun declareIdentifier(identifier: Identifier) {
		if(identifier is ClassIdentifier)
			throw SyntaxError("Can't declare a class inside of a class.")
		super.declareIdentifier(identifier)
	}
}