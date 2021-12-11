package errors.user

/**
 * Represents a source file ending unexpectedly.
 * This error is the programmers fault.
 */
class UnexpectedEndOfFileError(expectation: String): UserError("Unexpected end of file.\nExpected $expectation instead.")