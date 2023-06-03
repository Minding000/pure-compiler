package util

import code.Builder
import components.compiler.targets.llvm.LlvmGenericValue
import components.compiler.targets.llvm.LlvmProgram
import components.semantic_analysis.semantic_model.context.SemanticModelGenerator
import components.semantic_analysis.semantic_model.context.VariableTracker
import components.syntax_parser.element_generator.SyntaxTreeGenerator
import components.tokenizer.WordAtom
import components.tokenizer.WordGenerator
import logger.Severity
import source_structure.Module
import source_structure.Project
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

object TestUtil {
	const val TEST_FILE_NAME = "Test"
    private val defaultErrorStream = System.err
    private val testErrorStream = ByteArrayOutputStream()

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
		val project = Project("Test")
		val testModule = Module(project, "Test")
		testModule.addFile(LinkedList(), TEST_FILE_NAME, sourceCode)
		project.addModule(testModule)
		if(includeRequiredModules)
			Builder.loadRequiredModules(project)
		return project
	}

    fun parse(sourceCode: String, includeRequiredModules: Boolean = false, printReport: Boolean = true): ParseResult {
		val project = createTestProject(sourceCode, includeRequiredModules)
        val syntaxTreeGenerator = SyntaxTreeGenerator(project)
        val program = syntaxTreeGenerator.parseProgram()
		if(printReport)
        	syntaxTreeGenerator.project.context.logger.printReport(Severity.INFO)
        return ParseResult(syntaxTreeGenerator, program)
    }

    fun lint(sourceCode: String, includeRequiredModules: Boolean = false): LintResult {
        val parseResult = parse(sourceCode, includeRequiredModules, false)
		val context = parseResult.syntaxTreeGenerator.project.context
        val semanticModelGenerator = SemanticModelGenerator(context)
        val program = semanticModelGenerator.createSemanticModel(parseResult.program)
        context.logger.printReport(Severity.INFO)
        return LintResult(context, program)
    }

    fun run(sourceCode: String, entryPointPath: String): LlvmGenericValue {
		val lintResult = lint(sourceCode)
		val program = LlvmProgram("Test")
		val result: LlvmGenericValue
		try {
			program.loadSemanticModel(lintResult.program, entryPointPath)
			program.verify()
			program.compile()
			result = program.run()
		} catch(exception: Exception) {
			program.printIntermediateRepresentation()
			throw exception
		} finally {
			program.dispose()
		}
		return result
    }

	fun analyseDataFlow(sourceCode: String): VariableTracker {
		val parseResult = parse(sourceCode, includeRequiredModules = false, printReport = false)
		val context = parseResult.syntaxTreeGenerator.project.context
		val semanticModelGenerator = SemanticModelGenerator(context)
		val program = semanticModelGenerator.createSemanticModel(parseResult.program)
		context.logger.printReport(Severity.WARNING)
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

    fun assertSameSyntaxTree(expectedFileSyntaxTree: String, sourceCode: String) {
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

    private fun printDiffPosition(expected: String, actual: String) {
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
