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
		val additionalArguments = HashMap<String, String>()
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
					"--output-directory" -> {
						if(!argumentIterator.hasNext())
							exitWithParsingIssue("Missing output directory.")
						additionalArguments["output-directory"] = argumentIterator.next()
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
					exitWithParsingIssue("Too few positional arguments.", "run")
				if(positionalArguments.size > 3)
					exitWithParsingIssue("Too many positional arguments.", "run")
				if(additionalArguments.isNotEmpty())
					exitWithParsingIssue("Extraneous arguments: ${additionalArguments.map { argument -> argument.key }.joinToString(", ")}",
						"run")
				Builder.run(positionalArguments[1], positionalArguments[2])
			}
			"build" -> {
				if(positionalArguments.size < 3)
					exitWithParsingIssue("Too few positional arguments.", "build")
				if(positionalArguments.size > 3)
					exitWithParsingIssue("Too many positional arguments.", "build")
				Builder.build(positionalArguments[1], positionalArguments[2], additionalArguments["output-directory"])
			}
			"print" -> {
				if(positionalArguments.size < 4)
					exitWithParsingIssue("Too few positional arguments.", "print")
				if(positionalArguments.size > 4)
					exitWithParsingIssue("Too many positional arguments.", "print")
				val subject = positionalArguments[1].lowercase()
				if(!Builder.PRINT_SUBJECTS.contains(subject))
					exitWithParsingIssue("Unknown subject '$subject'.", "print")
				if(additionalArguments.isNotEmpty())
					exitWithParsingIssue("Extraneous arguments: ${additionalArguments.map { argument -> argument.key }.joinToString(", ")}",
						"print")
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
