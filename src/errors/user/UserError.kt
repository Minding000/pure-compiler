package errors.user

import java.lang.Exception

open class UserError(override val message: String): Exception(message)