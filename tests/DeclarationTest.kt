import org.junit.jupiter.api.Test

internal class DeclarationTest {

    @Test
    fun testVariableDeclaration() {
        val sourceCode = "var car"
        val expected =
            """
                Program {
                	Declaration {
                		Identifier { car }
                	}
                }
            """.trimIndent()
        TestUtil.assertAST(expected, sourceCode)
    }

    @Test
    fun testMultipleDeclarations() {
        val sourceCode = "var car, tire"
        val expected =
            """
                Program {
                	Declaration {
                		Identifier { car }
                		Identifier { tire }
                	}
                }
            """.trimIndent()
        TestUtil.assertAST(expected, sourceCode)
    }

    @Test
    fun testAssigningDeclaration() {
        val sourceCode = "var car = 5"
        val expected =
            """
                Program {
                	Declaration {
                		Assignment { Identifier { car } = NumberLiteral { 5 } }
                	}
                }
            """.trimIndent()
        TestUtil.assertAST(expected, sourceCode)
    }

    @Test
    fun testAssignment() {
        val sourceCode = "car = 5"
        val expected =
            """
                Program {
                	Assignment { Identifier { car } = NumberLiteral { 5 } }
                }
            """.trimIndent()
        TestUtil.assertAST(expected, sourceCode)
    }
}