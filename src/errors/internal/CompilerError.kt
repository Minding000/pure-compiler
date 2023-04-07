package errors.internal

import components.syntax_parser.syntax_tree.general.Element

/**
 * Represents an error that occurred during compilation.
 * This error is the compilers fault.
 */
class CompilerError(message: String, cause: Throwable? = null): InternalError(message, cause) {

	constructor(source: Element, message: String): this("${source.getStartString()}: $message")
}
