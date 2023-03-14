package errors.user

import source_structure.Section

/**
 * Represents an error in the syntax.
 * This error is the programmers fault.
 */
open class SyntaxError(message: String, val section: Section): UserError(message)
