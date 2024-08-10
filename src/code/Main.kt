package code

import logger.Severity
import java.util.*

object Main {
	//TODO these should probably not be global (this would simplify testing)
	var logLevel = Severity.INFO
	var shouldPrintCompileTimeDebugOutput = false
	var shouldWriteIntermediateRepresentation = false
	var shouldPrintRuntimeDebugOutput = false

	@JvmStatic
	fun main(arguments: Array<String>) {
		val positionalArguments = LinkedList<String>()
		val argumentIterator = arguments.iterator()
		for(argument in argumentIterator) {
			if(argument.startsWith("--")) {
				when(argument) {
					"--compile-time-debug-output" -> shouldPrintCompileTimeDebugOutput = true
					"--runtime-debug-output" -> shouldPrintRuntimeDebugOutput = true
					"--log-level" -> {
						if(!argumentIterator.hasNext()) {
							reportParsingIssue("Missing log level.")
							return
						}
						val logLevelInput = argumentIterator.next()
						try {
							logLevel = Severity.valueOf(logLevelInput.uppercase())
						} catch(exception: IllegalArgumentException) {
							reportParsingIssue("Unknown log level '$logLevelInput'.")
							if(shouldPrintCompileTimeDebugOutput)
								exception.printStackTrace()
							return
						}
					}
					else -> {
						reportParsingIssue("Unknown named argument '$argument'.")
						return
					}
				}
			} else {
				positionalArguments.add(argument)
			}
		}
		if(positionalArguments.isEmpty()) {
			reportParsingIssue("Missing sub-command.")
			return
		}
		when(val subCommand = positionalArguments.first()) {
			"run" -> {
				if(positionalArguments.size < 3) {
					reportParsingIssue("Too few arguments.", "run")
					return
				}
				if(positionalArguments.size > 3) {
					reportParsingIssue("Too many arguments.", "run")
					return
				}
				Builder.run(positionalArguments[1], positionalArguments[2])
			}
			"build" -> {
				if(positionalArguments.size < 3) {
					reportParsingIssue("Too few arguments.", "build")
					return
				}
				if(positionalArguments.size > 3) {
					reportParsingIssue("Too many arguments.", "build")
					return
				}
				Builder.build(positionalArguments[1], positionalArguments[2])
			}
			"print" -> {
				if(positionalArguments.size < 4) {
					reportParsingIssue("Too few arguments.", "print")
					return
				}
				if(positionalArguments.size > 4) {
					reportParsingIssue("Too many arguments.", "print")
					return
				}
				val subject = positionalArguments[1].lowercase()
				if(!Builder.PRINT_SUBJECTS.contains(subject)) {
					reportParsingIssue("Unknown subject '$subject'.", "print")
					return
				}
				Builder.print(subject, positionalArguments[2], positionalArguments[3])
			}
			"?",
			"help" -> {
				if(positionalArguments.size == 1) {
					Helper.help()
				} else {
					Helper.help(positionalArguments[1])
				}
			}
			else -> {
				reportParsingIssue("Sub-command '$subCommand' does not exist.")
			}
		}
	}

	private fun reportParsingIssue(message: String, subCommand: String = "") {
		System.err.println(message)
		Helper.help(subCommand)
	}
}
