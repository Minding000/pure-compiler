package errors

import java.lang.Exception

class CompilerError(message: String): Exception(message)