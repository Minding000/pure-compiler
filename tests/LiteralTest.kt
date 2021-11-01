import org.junit.jupiter.api.Test

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
        TestUtil.assertAST(expected, sourceCode)
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
        TestUtil.assertAST(expected, sourceCode)
    }
}