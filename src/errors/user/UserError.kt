package errors.user

open class UserError(override val message: String): Exception(message)
