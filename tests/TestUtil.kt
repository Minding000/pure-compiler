import parsing.element_generator.ElementGenerator
import errors.user.UserError
import source_structure.Module
import source_structure.Project
import util.indent
import util.stringify
import kotlin.test.assertContains
import kotlin.test.assertEquals

object TestUtil {

    private fun getAST(sourceCode: String): String {
        val project = Project("Test")
        val module = Module("Test")
        module.addFile("", "Test", sourceCode)
        project.addModule(module)
        return ElementGenerator(project).parseProgram().toString()
    }

    fun assertAST(expected_ast: String, sourceCode: String) {
        val actual_ast = getAST(sourceCode)
        val expected_ast = "Program {${"\n$expected_ast".indent()}\n}"
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
                print("Expected '${character.stringify()}' at $line:$index, but got '${actualChar?.stringify()}' instead.")
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