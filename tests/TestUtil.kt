import code.ElementGenerator
import objects.Project
import kotlin.test.assertEquals

object TestUtil {

    fun assertAST(expected_ast: String, sourceCode: String) {
        assertEquals(expected_ast, ElementGenerator(Project("Test", sourceCode)).parseProgram().toString())
    }
}