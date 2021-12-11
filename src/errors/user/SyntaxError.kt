package errors.user

/**
 * Represents an error in the syntax.
 * This error is the programmers fault.
 */
open class SyntaxError(message: String): UserError(message)