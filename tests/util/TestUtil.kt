package util

import code.Builder
import components.parsing.element_generator.ElementGenerator
import components.linting.Linter
import source_structure.Module
import source_structure.Project
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.StringBuilder
import java.util.*
import kotlin.test.assertEquals

object TestUtil {
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

    fun parse(sourceCode: String, includeRequiredModules: Boolean = false): ParseResult {
        val project = Project("Test")
        val testModule = Module("Test")
        testModule.addFile(LinkedList(), "Test", sourceCode)
        project.addModule(testModule)
        if(includeRequiredModules)
            Builder.loadRequiredModules(project)
        val elementGenerator = ElementGenerator(project)
        val program = elementGenerator.parseProgram()
        elementGenerator.logger.printReport()
        return ParseResult(elementGenerator, program)
    }

    fun lint(sourceCode: String, includeRequiredModules: Boolean = false): LintResult {
        val parseResult = parse(sourceCode, includeRequiredModules)
        val linter = Linter()
        val program = linter.lint(parseResult.program)
        linter.logger.printReport()
        return LintResult(linter, program)
    }

    fun assertSameSyntaxTree(expectedFileSyntaxTree: String, sourceCode: String) {
        val actualSyntaxTree = parse(sourceCode).program.toString()
        val expectedSyntaxTree = "Program {\n\tFile {${if(expectedFileSyntaxTree == "") ""
            else "\n$expectedFileSyntaxTree".indent().indent()}\n\t}\n}"
        printDiffPosition(expectedSyntaxTree, actualSyntaxTree)
        assertEquals(expectedSyntaxTree, actualSyntaxTree)
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
