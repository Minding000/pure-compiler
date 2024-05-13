package util

import code.Builder
import code.Main
import components.code_generation.llvm.LlvmProgram
import components.semantic_model.context.SemanticModelGenerator
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.syntax_parser.element_generator.SyntaxTreeGenerator
import components.tokenizer.WordAtom
import components.tokenizer.WordGenerator
import logger.Severity
import source_structure.Module
import source_structure.Project
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.TimeUnit
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

object TestUtil {
	const val TEST_PROJECT_NAME = "Test"
	const val TEST_MODULE_NAME = "Test"
	const val TEST_FILE_NAME = "Test"
    private val defaultErrorStream = System.err
    private val testErrorStream = ByteArrayOutputStream()
	private val EXTERNAL_FUNCTIONS = listOf("i32 @fprintf(ptr, ptr, ...)", "i32 @fflush(ptr)", "ptr @_fdopen(i32, ptr)",
		"i1 @__vcrt_initialize()", "i1 @__acrt_initialize()", "i32 @__acrt_initialize_stdio()", "ptr @__acrt_iob_func(i32)", "i32 @ferror(ptr)",
		"i32 @fclose(ptr)", "i64 @fwrite(ptr, i64, i64, ptr)", "i64 @fread(ptr, i64, i64, ptr)", "i32 @fgetc(ptr)", "void @Sleep(i32)",
		"void @exit(i32)", "ptr @memcpy(ptr, ptr, i32)", "void @llvm.va_start(ptr)", "void @llvm.va_copy(ptr, ptr)",
		"void @llvm.va_end(ptr)", "noalias ptr @malloc(i32)")

    fun recordErrorStream() {
        System.setErr(PrintStream(testErrorStream))
    }

    fun assertErrorStreamEmpty() {
        System.setErr(defaultErrorStream)
        val actualErrorStream = testErrorStream.toString()
        testErrorStream.reset()
        assertEquals("", actualErrorStream, "Expected error stream to be empty")
    }

	private fun createTestProject(sourceCode: String, includeRequiredModules: Boolean = false): Project {
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
        	syntaxTreeGenerator.project.context.logger.printReport(Severity.INFO)
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
			program.dispose()
			Main.shouldPrintRuntimeDebugOutput = previousFlag
		}
    }

	fun runAndReturnBoolean(sourceCode: String, entryPointPath: String): Boolean {
		val result: Boolean
		run(mapOf(TEST_FILE_NAME to sourceCode), entryPointPath, false, Builder.specialTypePaths) { program ->
			result = program.runAndReturnBoolean()
		}
		return result
	}

	fun runAndReturnByte(sourceCode: String, entryPointPath: String): Byte {
		val result: Byte
		run(mapOf(TEST_FILE_NAME to sourceCode), entryPointPath, false, Builder.specialTypePaths) { program ->
			result = program.runAndReturnByte()
		}
		return result
	}

	fun runAndReturnFloat(sourceCode: String, entryPointPath: String): Double {
		val result: Double
		run(mapOf(TEST_FILE_NAME to sourceCode), entryPointPath, false, Builder.specialTypePaths) { program ->
			result = program.runAndReturnFloat().toDouble()
		}
		return result
	}

	@JvmName("runTestFile")
	fun run(sourceCode: String, entryPointPath: String, specialTypePaths: Map<SpecialType, String>): Int {
		return run(sourceCode, entryPointPath, false, specialTypePaths.mapValues { (_, fileName) -> listOf(fileName) })
	}

	fun run(sourceCode: String, entryPointPath: String, specialTypePaths: Map<SpecialType, List<String>>): Int {
		return run(sourceCode, entryPointPath, false, specialTypePaths)
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
			println(intermediateRepresentation)
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

	fun runExecutable(path: String = ".\\out\\program.exe") {
		println("----- Program output: -----")
		val process = ProcessBuilder(path).inheritIO().start()
		val exitCode = process.onExit().join().exitValue()
		if(exitCode != ExitCode.SUCCESS)
			fail("Program exited with error code: $exitCode")
	}

	fun assertExecutablePrints(expectedString: String, input: String = "", path: String = ".\\out\\program.exe") {
		val process = ProcessBuilder(path).start()
		val reader = process.inputStream.bufferedReader()
		val outputStream = process.outputStream
		outputStream.write(input.toByteArray())
		outputStream.flush()
		outputStream.close()
		val timeoutInSeconds = 1L
		val exitCode = process.onExit().completeOnTimeout(null, timeoutInSeconds, TimeUnit.SECONDS).join()?.exitValue()
		val programFailed = exitCode != ExitCode.SUCCESS
		if(exitCode == null)
			System.err.println("Program timed out after ${timeoutInSeconds}s.")
		else if(programFailed)
			System.err.println("Program exited with error code: $exitCode")
		assertStringEquals(expectedString, reader.readText())
		if(programFailed)
			fail("Program failed! See lines above for details (timeout or error code).")
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
		if(externalGlobals.isNotEmpty()) {
			println("Possibly non-existent globals (${externalGlobals.size}):")
			externalGlobals.forEach { global -> println(global) }
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

	fun assertTokenType(sourceCode: String, type: WordAtom) {
		val project = createTestProject(sourceCode)
		val wordGenerator = WordGenerator(project)
		val word = wordGenerator.getNextWord()
		assertNotNull(word, "No token found")
		assertEquals(sourceCode, word.getValue(), "The generated token doesn't match the entire input")
		assertEquals(type, word.type, "The generated token doesn't match the expected type")
	}

    fun assertSyntaxTreeEquals(expectedFileSyntaxTree: String, sourceCode: String) {
        val actualSyntaxTree = parse(sourceCode).program.toString()
        val expectedSyntaxTree = "Program {\n\tFile {${if(expectedFileSyntaxTree == "") ""
            else "\n$expectedFileSyntaxTree".indent().indent()}\n\t}\n}"
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
