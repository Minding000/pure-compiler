package errors.internal

open class InternalError(message: String, cause: Throwable? = null): Exception(message, cause)
