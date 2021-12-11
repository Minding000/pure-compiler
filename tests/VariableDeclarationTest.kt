import org.junit.jupiter.api.Test

internal class VariableDeclarationTest {

	@Test
	fun testVariableDeclaration() {
		val sourceCode = "var car: Int"
		val expected =
			"""
				Program {
					Declaration {
						VariableIdentifier { car }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testMultipleDeclarations() {
		val sourceCode = "var car: String, tire: Int"
		val expected =
			"""
				Program {
					Declaration {
						VariableIdentifier { car }
						VariableIdentifier { tire }
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
						Assignment { VariableIdentifier { car } = NumberLiteral { 5 } }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testAssignment() {
		val sourceCode =
			"""
				var car: Int
				car = 5
            """.trimIndent()
		val expected =
			"""
				Program {
					Declaration {
						VariableIdentifier { car }
					}
					Assignment { VariableReference { car } = NumberLiteral { 5 } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testRedeclarationError() {
		val sourceCode =
			"""
				var car: Int
				car = 5
				var a: String, car: Int
            """.trimIndent()
		TestUtil.assertUserError("Cannot redeclare identifier 'car'", sourceCode)
	}
}