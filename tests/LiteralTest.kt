import code.ElementGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class LiteralTest {

    @Test
    fun testNumberLiteral() {
        val sourceCode = "345"
        val expected =
            """
                Program {	
                	NumberLiteral { 345 }
                }
            """.trimIndent()
        assertEquals(expected, ElementGenerator(sourceCode).parseProgram().toString())
    }

    @Test
    fun testStringLiteral() {
        val sourceCode = "\"hello world!\""
        val expected =
            """
                Program {	
                	StringLiteral { "hello world!" }
                }
            """.trimIndent()
        assertEquals(expected, ElementGenerator(sourceCode).parseProgram().toString())
    }
}