package errors.user

/**
 * Represents an incorrect context being encountered.
 * This error is the programmers fault.
 */
class IncorrectContextError(message: String): SyntaxError(message)
