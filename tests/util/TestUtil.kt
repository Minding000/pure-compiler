package util

import code.Builder
import parsing.element_generator.ElementGenerator
import errors.user.UserError
import linting.Linter
import parsing.syntax_tree.general.Program
import source_structure.Module
import source_structure.Project
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.StringBuilder
import java.util.*
import kotlin.test.assertContains
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

    private fun parseProgram(sourceCode: String, includeRequiredModules: Boolean = false): Program {
        val project = Project("Test")
        val testModule = Module("Test")
        testModule.addFile(LinkedList(), "Test", sourceCode)
        project.addModule(testModule)
        if(includeRequiredModules)
            Builder.loadRequiredModules(project)
        return ElementGenerator(project).parseProgram()
    }

    private fun getAST(sourceCode: String): String {
        return parseProgram(sourceCode).toString()
    }

    fun lint(sourceCode: String, includeRequiredModules: Boolean): LintResult {
        val linter = Linter()
        val program = linter.lint(parseProgram(sourceCode, includeRequiredModules))
        linter.printMessages()
        return LintResult(linter, program)
    }

    fun assertAST(expected_ast: String, sourceCode: String) {
        val actualAst = getAST(sourceCode)
        val expectedAst = "Program {\n\tFile {${"\n$expected_ast".indent().indent()}\n\t}\n}"
        printDiffPosition(expectedAst, actualAst)
        assertEquals(expectedAst, actualAst)
    }

    fun assertUserError(expectedMessage: String, sourceCode: String) {
        var actualMessage = ""
        try {
            getAST(sourceCode)
        } catch(e: UserError) {
            actualMessage = e.message ?: ""
        }
        printDiffPosition(expectedMessage, actualMessage)
        assertContains(actualMessage, expectedMessage)
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