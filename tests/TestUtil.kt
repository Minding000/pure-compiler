import code.ElementGenerator
import errors.user.UserError
import source_structure.Project
import kotlin.test.assertContains
import kotlin.test.assertEquals

object TestUtil {

    fun assertAST(expected_ast: String, sourceCode: String) {
        val actual_ast = ElementGenerator(Project("Test", sourceCode)).parseProgram().toString()
        printDiffPosition(expected_ast, actual_ast)
        assertEquals(expected_ast, actual_ast)
    }

    fun assertUserError(expected_message: String, sourceCode: String) {
        var actual_message = ""
        try {
            ElementGenerator(Project("Test", sourceCode)).parseProgram().toString()
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
            if(character != actual[position]) {
                print("Expected '$character' at $line:$index, but got '${actual[position]}' instead.")
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