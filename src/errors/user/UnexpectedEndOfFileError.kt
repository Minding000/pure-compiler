package errors.user

import source_structure.Section

/**
 * Represents a source file ending unexpectedly.
 * This error is the programmers fault.
 */
class UnexpectedEndOfFileError(expectation: String, section: Section):
	SyntaxError("Unexpected end of file '${section.start.line.file.getIdentifier()}'.\nExpected $expectation instead.", section)
