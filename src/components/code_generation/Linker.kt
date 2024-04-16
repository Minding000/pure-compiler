package components.code_generation

import errors.internal.CompilerError
import util.ExitCode

object Linker {

	fun link(objectFilePath: String, executableFilePath: String) {
		// see: https://stackoverflow.com/questions/64413414/unresolved-external-symbol-printf-in-windows-x64-assembly-programming-with-nasm
		val process = ProcessBuilder("D:\\Programme\\LLVM\\bin\\lld-link.exe", objectFilePath, "/out:$executableFilePath",
			"/subsystem:console", "/defaultlib:msvcrt", "legacy_stdio_definitions.lib").inheritIO().start()
		val exitCode = process.onExit().join().exitValue()
		if(exitCode != ExitCode.SUCCESS)
			throw CompilerError("Failed to link object file. Exit code #$exitCode")
	}
}
