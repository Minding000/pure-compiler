package errors.internal

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

/**
 * Represents an error that occurred during compilation.
 * This error is the compilers fault.
 */
class CompilerError(message: String, cause: Throwable? = null): InternalError(message, cause) {

	constructor(source: SyntaxTreeNode?, message: String): this(if(source == null) message else "${source.getStartString()}: $message")
}
