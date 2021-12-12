package code

object Helper {
	fun help(input: String? = null) {
		when(input) {
			else -> printHelp()
		}
	}

	fun printHelp() {
		println("List of sub-commands:")
		println(" - build")
		println(" - help")
	}
}