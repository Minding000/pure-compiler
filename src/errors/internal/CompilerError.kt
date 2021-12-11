package errors.internal

/**
 * Represents an error that occurred during compilation.
 * This error is the compilers fault.
 */
class CompilerError(message: String): InternalError(message)