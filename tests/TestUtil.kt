import parsing.element_generator.ElementGenerator
import errors.user.UserError
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
import kotlin.test.assertTrue

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

    private fun getAST(sourceCode: String): String {
        val project = Project("Test")
        val module = Module("Test")
        module.addFile(LinkedList(), "Test", sourceCode)
        project.addModule(module)
        return ElementGenerator(project).parseProgram().toString()
    }

    fun assertAST(expected_ast: String, sourceCode: String) {
        val actual_ast = getAST(sourceCode)
        val expected_ast = "Program {\n\tFile {${"\n$expected_ast".indent().indent()}\n\t}\n}"
        printDiffPosition(expected_ast, actual_ast)
        assertEquals(expected_ast, actual_ast)
    }

    fun assertUserError(expected_message: String, sourceCode: String) {
        var actual_message = ""
        try {
            getAST(sourceCode)
        } catch(e: UserError) {
            actual_message = e.message ?: ""
        }
        printDiffPosition(expected_message, actual_message)
        assertContains(actual_message, expected_message)
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