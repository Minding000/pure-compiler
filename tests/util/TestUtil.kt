package util

import code.Builder
import code.Main
import components.code_generation.llvm.wrapper.LlvmProgram
import components.semantic_model.context.SemanticModelGenerator
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.syntax_parser.element_generator.SyntaxTreeGenerator
import components.tokenizer.StateStack
import components.tokenizer.WordAtom
import components.tokenizer.WordGenerator
import logger.Severity
import source_structure.Module
import source_structure.Project
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.concurrent.TimeUnit
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.test.*

object TestUtil {
	const val EXAMPLE_PROJECTS_PATH = "D:\\Daten\\Projekte\\Pure\\Example projects"
	const val TEST_PROJECT_NAME = "Test"
	const val TEST_MODULE_NAME = "Test"
	const val TEST_FILE_NAME = "Test"
	private val defaultErrorStream = System.err
	private val testErrorStream = ByteArrayOutputStream()
	private val EXTERNAL_GLOBALS = listOf("@environ = external global ptr")
	private val EXTERNAL_FUNCTIONS = listOf("i32 @fprintf(ptr, ptr, ...)", "i32 @sprintf(ptr, ptr, ...)",
		"i32 @snprintf(ptr, i64, ptr, ...)", "i32 @_snprintf(ptr, i64, ptr, ...)", "double @strtod(ptr, ptr)", "i32 @fflush(ptr)",
		"ptr @fdopen(i32, ptr)", "ptr @_fdopen(i32, ptr)", "i32 @_tmainCRTStartup()", "i1 @__vcrt_initialize()", "i1 @__acrt_initialize()",
		"i32 @__acrt_initialize_stdio()", "ptr @__acrt_iob_func(i32)", "i32 @ferror(ptr)", "i32 @fclose(ptr)",
		"i64 @fwrite(ptr, i64, i64, ptr)", "i64 @fread(ptr, i64, i64, ptr)", "i32 @fgetc(ptr)", "void @Sleep(i32)", "void @exit(i32)",
		"ptr @memcpy(ptr, ptr, i64)", "void @llvm.va_start(ptr)", "void @llvm.va_start.p0(ptr)", "void @llvm.va_copy(ptr, ptr)",
		"void @llvm.va_end(ptr)", "void @llvm.va_end.p0(ptr)", "{ i32, i1 } @llvm.sadd.with.overflow.i32(i32, i32)",
		"{ i32, i1 } @llvm.ssub.with.overflow.i32(i32, i32)", "{ i32, i1 } @llvm.smul.with.overflow.i32(i32, i32)",
		"noalias ptr @malloc(i32)", "{ i8, i1 } @llvm.sadd.with.overflow.i8(i8, i8)", "{ i8, i1 } @llvm.ssub.with.overflow.i8(i8, i8)",
		"{ i8, i1 } @llvm.smul.with.overflow.i8(i8, i8)", "noalias ptr @malloc(i32)", "i64 @strlen(ptr)", "ptr @strchr(ptr, i32)",
		"ptr @GetEnvironmentStrings()", "i32 @FreeEnvironmentStrings(ptr)")

	fun assertErrorStreamEmpty(runnable: Runnable) {
		System.setErr(PrintStream(testErrorStream))
		var exception: Exception? = null
		try {
			Main.shouldThrowInsteadOfExit = true
			runnable.run()
		} catch(localException: Exception) {
			exception = localException
		}
		System.setErr(defaultErrorStream)
		val actualErrorStream = testErrorStream.toString()
		testErrorStream.reset()
		if(exception != null) {
			if(exception.message == "exitWithError() called") {
				fail("exitWithError() called")
			} else {
				fail("main function threw", exception)
			}
		}
		assertEquals("", actualErrorStream, "Expected error stream to be empty")
	}

	fun createTestProject(sourceCode: String, includeRequiredModules: Boolean = false): Project {
		return createTestProject(mapOf(TEST_FILE_NAME to sourceCode), includeRequiredModules)
	}

	private fun createTestProject(files: Map<String, String>, includeRequiredModules: Boolean = false): Project {
		val project = Project(TEST_PROJECT_NAME)
		val testModule = Module(project, TEST_MODULE_NAME)
		for((name, content) in files)
			testModule.addFile(emptyList(), name, content)
		project.addModule(testModule)
		if(includeRequiredModules)
			Builder.loadRequiredModules(project)
		return project
	}

	fun parse(sourceCode: String, includeRequiredModules: Boolean = false, printReport: Boolean = true): ParseResult {
		return parse(mapOf(TEST_FILE_NAME to sourceCode), includeRequiredModules, printReport)
	}

	fun parse(files: Map<String, String>, includeRequiredModules: Boolean = false, printReport: Boolean = true): ParseResult {
		val project = createTestProject(files, includeRequiredModules)
		val syntaxTreeGenerator = SyntaxTreeGenerator(project)
		val program = syntaxTreeGenerator.parseProgram()
		if(printReport)
			syntaxTreeGenerator.project.context.logger.printReport(Severity.INFO, false)
		return ParseResult(syntaxTreeGenerator, program)
	}

	fun lint(sourceCode: String, includeRequiredModules: Boolean = false, printReport: Boolean = true,
			 specialTypePaths: Map<SpecialType, List<String>> = Builder.specialTypePaths): LintResult {
		return lint(mapOf(TEST_FILE_NAME to sourceCode), includeRequiredModules, printReport, specialTypePaths)
	}

	fun lint(files: Map<String, String>, includeRequiredModules: Boolean = false, printReport: Boolean = true,
			 specialTypePaths: Map<SpecialType, List<String>> = Builder.specialTypePaths): LintResult {
		val parseResult = parse(files, includeRequiredModules, false)
		val context = parseResult.syntaxTreeGenerator.project.context
		val semanticModelGenerator = SemanticModelGenerator(context)
		val program = semanticModelGenerator.createSemanticModel(parseResult.program, specialTypePaths)
		if(printReport)
			context.logger.printReport(Severity.INFO,
				!includeRequiredModules && specialTypePaths == Builder.specialTypePaths)
		return LintResult(context, program)
	}

	fun getIntermediateRepresentation(sourceCode: String): String {
		val includeRequiredModules = false
		val lintResult = lint(sourceCode, includeRequiredModules, false)
		val program = LlvmProgram(TEST_PROJECT_NAME)
		val previousFlag = Main.shouldPrintRuntimeDebugOutput
		Main.shouldPrintRuntimeDebugOutput = false
		try {
			program.loadSemanticModel(lintResult.program)
			lintResult.context.logger.printReport(Severity.INFO, !includeRequiredModules)
			program.verify()
			return program.getIntermediateRepresentation()
		} finally {
			println(program.getIntermediateRepresentation())
			program.dispose()
			Main.shouldPrintRuntimeDebugOutput = previousFlag
		}
	}

	fun runAndReturnBoolean(sourceCode: String, entryPointPath: String,
							specialTypePaths: Map<SpecialType, List<String>> = Builder.specialTypePaths): Boolean {
		val result: Boolean
		run(mapOf(TEST_FILE_NAME to sourceCode), entryPointPath, false, specialTypePaths) { program ->
			result = program.runAndReturnBoolean()
		}
		return result
	}

	fun runAndReturnByte(sourceCode: String, entryPointPath: String,
						 specialTypePaths: Map<SpecialType, List<String>> = Builder.specialTypePaths): Byte {
		val result: Byte
		run(mapOf(TEST_FILE_NAME to sourceCode), entryPointPath, false, specialTypePaths) { program ->
			result = program.runAndReturnByte()
		}
		return result
	}

	fun runAndReturnFloat(sourceCode: String, entryPointPath: String,
						  specialTypePaths: Map<SpecialType, List<String>> = Builder.specialTypePaths): Double {
		val result: Double
		run(mapOf(TEST_FILE_NAME to sourceCode), entryPointPath, false, specialTypePaths) { program ->
			result = program.runAndReturnFloat().toDouble()
		}
		return result
	}

	fun run(sourceCode: String, entryPointPath: String, specialTypePaths: List<SpecialType>, source: String = TEST_FILE_NAME): Int {
		return run(sourceCode, entryPointPath, false, specialTypePaths.associateWith { listOf(source) })
	}

	fun run(sourceCode: String, entryPointPath: String, includeRequiredModules: Boolean = false,
			specialTypePaths: Map<SpecialType, List<String>> = Builder.specialTypePaths): Int {
		return run(mapOf(TEST_FILE_NAME to sourceCode), entryPointPath, includeRequiredModules, specialTypePaths)
	}

	fun run(files: Map<String, String>, entryPointPath: String, includeRequiredModules: Boolean = false,
			specialTypePaths: Map<SpecialType, List<String>> = Builder.specialTypePaths): Int {
		val result: Int
		run(files, entryPointPath, includeRequiredModules, specialTypePaths) { program ->
			result = program.runAndReturnInt()
		}
		return result
	}

	@OptIn(ExperimentalContracts::class)
	fun run(files: Map<String, String>, entryPointPath: String, includeRequiredModules: Boolean = false,
			specialTypePaths: Map<SpecialType, List<String>> = Builder.specialTypePaths, runner: (program: LlvmProgram) -> Unit) {
		contract {
			callsInPlace(runner, InvocationKind.EXACTLY_ONCE)
		}
		val lintResult = lint(files, includeRequiredModules, false, specialTypePaths)
		val program = LlvmProgram(TEST_PROJECT_NAME)
		try {
			try {
				program.loadSemanticModel(lintResult.program, entryPointPath)
			} finally {
				lintResult.context.logger.printReport(Severity.INFO,
					!includeRequiredModules && specialTypePaths == Builder.specialTypePaths)
			}
			val intermediateRepresentation = program.getIntermediateRepresentation()
			print(intermediateRepresentation)
			println("----------")
			program.verify()
			program.compile()
			printDiagnostics(intermediateRepresentation)
			println("----------")
			runner(program)
		} finally {
			program.dispose()
		}
	}

	fun runExecutable(path: String = ".${File.separator}out${File.separator}program.exe") {
		println("----- Program output: -----")
		val process = ProcessBuilder(path).inheritIO().start()
		val exitCode = process.waitFor()
		if(exitCode != ExitCode.SUCCESS)
			fail(createExitCodeMessage(exitCode, ExitCode.SUCCESS))
	}

	fun assertExecutablePrintsLine(expectedString: String, input: String = "",
								   path: String = ".${File.separator}out${File.separator}program.exe",
								   expectedExitCode: Int = ExitCode.SUCCESS) {
		assertExecutablePrints("$expectedString\n", input, path, expectedExitCode)
	}

	fun assertExecutablePrints(expectedOutput: String, input: String = "",
							   path: String = ".${File.separator}out${File.separator}program.exe",
							   expectedExitCode: Int = ExitCode.SUCCESS,
							   environmentVariables: Map<String, String> = emptyMap(),
							   arguments: List<String> = emptyList()) {
		val processBuilder = ProcessBuilder(path, *arguments.toTypedArray())
		processBuilder.environment().clear()
		processBuilder.environment().putAll(environmentVariables)
		val process = processBuilder.start()
		val outputStreamReader = process.inputStream.bufferedReader()
		val errorStreamReader = process.errorStream.bufferedReader()
		if(input.isNotEmpty()) {
			val inputStream = process.outputStream
			inputStream.write(input.toByteArray())
			inputStream.flush()
			inputStream.close()
		}
		val timeoutInSeconds = 1L
		val processFuture = process.onExit().completeOnTimeout(null, timeoutInSeconds, TimeUnit.SECONDS)
		val outputStreamBuilder = StringBuilder()
		val errorStreamBuilder = StringBuilder()
		val outputBuffer = CharArray(512)
		while(true) {
			val isDone = processFuture.isDone
			while(outputStreamReader.ready()) {
				val byteCount = outputStreamReader.read(outputBuffer)
				outputStreamBuilder.appendRange(outputBuffer, 0, byteCount)
			}
			while(errorStreamReader.ready()) {
				val byteCount = errorStreamReader.read(outputBuffer)
				errorStreamBuilder.appendRange(outputBuffer, 0, byteCount)
			}
			if(isDone)
				break
			try {
				Thread.sleep(20)
			} catch(_: InterruptedException) {
			}
		}
		val exitCode = processFuture.join()?.exitValue()
		val programFailed = exitCode != expectedExitCode
		if(exitCode == null) {
			System.err.println("Program timed out after ${timeoutInSeconds}s.")
			process.waitFor(2, TimeUnit.SECONDS)
			process.destroyForcibly()
		} else if(programFailed) {
			System.err.println(createExitCodeMessage(exitCode, expectedExitCode))
		}
		val errorStream = errorStreamBuilder.toString()
		if(errorStream.isNotEmpty())
			System.err.println(errorStream)
		assertStringEquals(expectedOutput.replace("\n", System.lineSeparator()), outputStreamBuilder.toString())
		assertTrue(errorStream.isEmpty(), "The error stream is not empty.")
		if(programFailed)
			fail("Program failed! See lines above for details (timeout or error code).")
	}

	/**
	 * Windows exit codes: https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-erref/596a1078-e883-4972-9bbc-49e60bebca55
	 */
	private fun createExitCodeMessage(exitCode: Int, expectedExitCode: Int): String {
		return "Program exited with exit code: $exitCode (0x${
			exitCode.toUInt().toString(16).uppercase()
		}) - expected $expectedExitCode instead"
	}

	private fun printDiagnostics(intermediateRepresentation: String) {
		val lines = intermediateRepresentation.split("\n")
		val declaredFunctions = lines.filter { line -> line.startsWith("declare") }
		val functionsMissingAnImplementation = declaredFunctions.filter { declaredFunction ->
			val identifier = declaredFunction.substringAfter("declare ").substringBefore(")") + ")"
			!EXTERNAL_FUNCTIONS.contains(identifier)
		}
		if(functionsMissingAnImplementation.isNotEmpty()) {
			println("Possibly unimplemented functions (${functionsMissingAnImplementation.size}):")
			functionsMissingAnImplementation.forEach { function -> println(function) }
			fail("Encountered unimplemented functions. Check output above for details.")
		}
		val externalGlobals = lines.filter { line -> line.contains("external global") }
		val externalGlobalsMissingADefinition = externalGlobals.filter { externalGlobal -> !EXTERNAL_GLOBALS.contains(externalGlobal) }
		if(externalGlobalsMissingADefinition.isNotEmpty()) {
			println("Possibly non-existent globals (${externalGlobalsMissingADefinition.size}):")
			externalGlobalsMissingADefinition.forEach { global -> println(global) }
			fail("Encountered non-existent globals. Check output above for details.")
		}
	}

	fun analyseDataFlow(sourceCode: String): VariableTracker {
		val parseResult = parse(sourceCode, includeRequiredModules = false, printReport = false)
		val context = parseResult.syntaxTreeGenerator.project.context
		val semanticModelGenerator = SemanticModelGenerator(context)
		val program = semanticModelGenerator.createSemanticModel(parseResult.program, Builder.specialTypePaths)
		context.logger.printReport(Severity.WARNING, true)
		val testFile = program.files.find { file -> file.file.name == TEST_FILE_NAME }
		assertNotNull(testFile, "Missing test file")
		return testFile.variableTracker
	}

	fun assertTokenIsIgnored(sourceCode: String) {
		val project = createTestProject(sourceCode)
		val wordGenerator = WordGenerator(project)
		val word = wordGenerator.getNextWord()
		assertNull(word?.getValue(), "Token is not ignored")
	}

	fun assertTokenType(sourceCode: String, type: WordAtom, stack: StateStack? = null) {
		val project = createTestProject(sourceCode)
		val wordGenerator = WordGenerator(project, stack)
		val word = wordGenerator.getNextWord()
		assertNotNull(word, "No token found")
		assertEquals(sourceCode, word.getValue(), "The generated token doesn't match the entire input")
		assertEquals(type, word.type, "The generated token doesn't match the expected type")
	}

	fun assertSyntaxTreeEquals(expectedFileSyntaxTree: String, sourceCode: String) {
		val actualSyntaxTree = parse(sourceCode).program.toString()
		val expectedSyntaxTree = "Program {\n\tFile [ $TEST_MODULE_NAME.$TEST_FILE_NAME ] {${
			if(expectedFileSyntaxTree == "") ""
			else "\n$expectedFileSyntaxTree".indent().indent()
		}\n\t}\n}"
		assertStringEquals(expectedSyntaxTree, actualSyntaxTree)
	}

	fun assertStringEquals(expected: String, actual: String?) {
		if(actual != null && actual != expected)
			printDiffPosition(expected, actual)
		assertEquals(expected, actual)
	}

	fun printDiffPosition(expected: String, actual: String) {
		var position = 0
		var line = 1
		var index = 1
		for(character in expected) {
			val actualChar = actual.getOrNull(position)
			if(character != actualChar) {
				val highlight = StringBuilder()
				val start = position - index + 1
				var end = expected.indexOf('\n', start)
				if(end == -1)
					end = expected.length
				highlight.append(expected.substring(start, end).replace("\t", " "))
				highlight.append("\n")
				highlight.append(" ".repeat(index - 1))
				highlight.append("^")
				println("Expected '${character.stringify()}' at $line:$index, but got '${actualChar?.stringify()}' instead.")
				println(highlight.toString())
				break
			}
			position++
			if(character == '\n') {
				line++
				index = 1
			} else {
				index++
			}
		}
	}
}
