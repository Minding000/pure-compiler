package code

import logger.Severity
import java.util.*
import kotlin.system.exitProcess

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
						if(!argumentIterator.hasNext())
							exitWithParsingIssue("Missing log level.")
						val logLevelInput = argumentIterator.next()
						try {
							logLevel = Severity.valueOf(logLevelInput.uppercase())
						} catch(exception: IllegalArgumentException) {
							if(shouldPrintCompileTimeDebugOutput)
								exception.printStackTrace()
							exitWithParsingIssue("Unknown log level '$logLevelInput'.")
						}
					}
					else -> {
						exitWithParsingIssue("Unknown named argument '$argument'.")
					}
				}
			} else {
				positionalArguments.add(argument)
			}
		}
		if(positionalArguments.isEmpty())
			exitWithParsingIssue("Missing sub-command.")
		when(val subCommand = positionalArguments.first()) {
			"run" -> {
				if(positionalArguments.size < 3)
					exitWithParsingIssue("Too few arguments.", "run")
				if(positionalArguments.size > 3)
					exitWithParsingIssue("Too many arguments.", "run")
				Builder.run(positionalArguments[1], positionalArguments[2])
			}
			"build" -> {
				if(positionalArguments.size < 3)
					exitWithParsingIssue("Too few arguments.", "build")
				if(positionalArguments.size > 3)
					exitWithParsingIssue("Too many arguments.", "build")
				Builder.build(positionalArguments[1], positionalArguments[2])
			}
			"print" -> {
				if(positionalArguments.size < 4)
					exitWithParsingIssue("Too few arguments.", "print")
				if(positionalArguments.size > 4)
					exitWithParsingIssue("Too many arguments.", "print")
				val subject = positionalArguments[1].lowercase()
				if(!Builder.PRINT_SUBJECTS.contains(subject))
					exitWithParsingIssue("Unknown subject '$subject'.", "print")
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
				exitWithParsingIssue("Sub-command '$subCommand' does not exist.")
			}
		}
	}

	private fun exitWithParsingIssue(message: String, subCommand: String = ""): Nothing {
		System.err.println(message)
		Helper.help(subCommand)
		exitWithError()
	}

	fun exitWithError(message: String): Nothing {
		System.err.println(message)
		exitWithError()
	}

	fun exitWithError(): Nothing {
		exitProcess(1)
	}
}
