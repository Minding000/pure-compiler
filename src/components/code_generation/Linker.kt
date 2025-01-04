package components.code_generation

import errors.internal.CompilerError
import util.ExitCode

object Linker {

	fun link(objectFilePath: String, executableFilePath: String) {
		// see: https://stackoverflow.com/questions/64413414/unresolved-external-symbol-printf-in-windows-x64-assembly-programming-with-nasm
		val arguments = mutableListOf("lld-link", objectFilePath, "/out:$executableFilePath")
		if(isRunningOnWindows())
			arguments.addAll(listOf("/subsystem:console", "/defaultlib:msvcrt", "legacy_stdio_definitions.lib"))
		val process = ProcessBuilder(arguments).inheritIO().start()
		val exitCode = process.waitFor()
		if(exitCode != ExitCode.SUCCESS)
			throw CompilerError("Failed to link object file. Exit code #$exitCode")
	}

	private fun isRunningOnWindows(): Boolean {
		return System.getProperty("os.name").lowercase().contains("windows")
	}
}
