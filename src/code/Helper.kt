package code

import logger.Severity

object Helper {
	fun help(input: String? = null) {
		when(input) {
			"run" -> println("Usage: run <path> <entrypoint>")
			"build" -> println("Usage: build <path> <entrypoint>")
			"print" -> println("Usage: print source|ast|llvm-ir <path> <entrypoint>")
			else -> printHelp()
		}
	}

	private fun printHelp() {
		println("List of sub-commands:")
		println(" - run")
		println(" - build")
		println(" - print")
		println(" - help [<sub-command>]")
		println("List of options:")
		println(" - --compile-time-debug-output")
		println(" - --runtime-debug-output")
		println(" - --log-level ${Severity.entries.joinToString("|") { severity -> severity.name.lowercase() }}")
	}
}
