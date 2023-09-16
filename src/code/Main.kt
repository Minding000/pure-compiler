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
				if(args.size < 3) {
					System.err.println("Too few arguments.")
					Helper.help("build")
					return
				}
				if(args.size > 3) {
					System.err.println("Too many arguments.")
					Helper.help("build")
					return
				}
				Builder.build(args[1], args[2])
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
