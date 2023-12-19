package code

object Helper {
	fun help(input: String? = null) {
		when(input) {
			"run" -> println("Usage: run <path> <entrypoint>")
			"build" -> println("Usage: build <path> <entrypoint>")
			else -> printHelp()
		}
	}

	private fun printHelp() {
		println("List of sub-commands:")
		println(" - run")
		println(" - build")
		println(" - help")
	}
}
