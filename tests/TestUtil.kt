import code.Builder
import parsing.element_generator.ElementGenerator
import errors.user.UserError
import linter.Linter
import linter.messages.Message
import parsing.ast.general.Program
import source_structure.Module
import source_structure.Project
import util.indent
import util.stringify
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.StringBuilder
import java.util.*
import kotlin.test.assertContains
import kotlin.test.assertEquals

object TestUtil {
    private val defaultErrorStream = System.err
    private val testErrorStream = ByteArrayOutputStream()
    var includeRequiredModules = false

    fun recordErrorStream() {
        System.setErr(PrintStream(testErrorStream))
    }

    fun assertErrorStreamEmpty() {
        System.setErr(defaultErrorStream)
        val actualErrorStream = testErrorStream.toString()
        testErrorStream.reset()
        assertEquals("", actualErrorStream, "Expected error stream to be empty")
    }

    private fun parseProgram(sourceCode: String): Program {
        val project = Project("Test")
        val testModule = Module("Test")
        testModule.addFile(LinkedList(), "Test", sourceCode)
        project.addModule(testModule)
        if(includeRequiredModules) {
            Builder.loadRequiredModules(project)
            includeRequiredModules = false
        }
        return ElementGenerator(project).parseProgram()
    }

    private fun getAST(sourceCode: String): String {
        return parseProgram(sourceCode).toString()
    }

    private fun getLinter(sourceCode: String): Linter {
        val linter = Linter()
        linter.lint(parseProgram(sourceCode))
        return linter
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

    fun assertLinterMessage(expectedType: Message.Type, expectedMessage: String, sourceCode: String) {
        val linter = getLinter(sourceCode)
        for(message in linter.messages) {
            if(message.description.contains(expectedMessage)) {
                if(message.type != expectedType)
                    throw AssertionError("Linter message '$expectedMessage' has type '${message.type}' instead of expected type '$expectedType'.")
                return
            }
        }
        linter.printMessages()
        throw AssertionError("Expected linter message '$expectedMessage' hasn't been emitted.")
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