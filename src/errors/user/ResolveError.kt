package errors.user

import elements.identifier.Identifier
import elements.identifier.IdentifierReference

/**
 * Represents an error in the identifier usage.
 * This error is the programmers fault.
 */
open class ResolveError(message: String): UserError(message) {

	constructor(reference: IdentifierReference<out Identifier>): this("Trying to access undeclared identifier '${reference.getValue()}' at ${reference.getRegionString()}.")
}