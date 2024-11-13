package errors.internal

import components.code_generation.llvm.models.general.Unit
import components.semantic_model.general.SemanticModel
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode

/**
 * Represents an error that occurred during compilation.
 * This error is the compilers fault.
 */
class CompilerError(message: String, cause: Throwable? = null): InternalError(message, cause) {

	constructor(source: SyntaxTreeNode?, message: String): this(if(source == null) message else "${source.getStartString()}: $message")

	constructor(model: SemanticModel, message: String): this(model.source, message)

	constructor(unit: Unit, message: String): this(unit.model.source, message)
}
