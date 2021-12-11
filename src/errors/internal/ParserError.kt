package errors.internal

/**
 * Represents an error that occurred during parsing.
 * This error is the parsers fault.
 */
class ParserError(message: String): InternalError(message)