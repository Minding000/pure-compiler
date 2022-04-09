package code

object Main {
	const val DEBUG = true

	@JvmStatic
	fun main(args: Array<String>) {
		if(args.isEmpty()) {
			Helper.help()
			return
		}
		when(val subCommand = args.first()) {
			"build" -> {
				if(args.size < 2) {
					println("Please provide a file or directory to build.")
					Helper.help("build")
					return
				}
				if(args.size > 2) {
					println("Too many arguments.")
					Helper.help("build")
					return
				}
				Builder.build(args[1])
			}
			"?",
			"help" -> {
				Helper.help()
			}
			else -> {
				println("Sub-command '$subCommand' does not exist.")
			}
		}
	}
}