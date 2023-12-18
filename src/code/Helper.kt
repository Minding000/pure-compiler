package code

object Helper {
	fun help(input: String? = null) {
		when(input) {
			"build" -> println("Usage: build <path> <entrypoint>")
			else -> printHelp()
		}
	}

	private fun printHelp() {
		println("List of sub-commands:")
		println(" - build")
		println(" - help")
	}
}
