import org.junit.jupiter.api.Test

internal class FunctionDefinitionTest {

	@Test
	fun testFunctionDeclaration() {
		val sourceCode = "class Animal { fun getSound(loudness: Int) {  } }"
		val expected =
			"""
				Program {
					Class [ClassIdentifier { Animal }] {
						Function [FunctionIdentifier { getSound }(
							VariableIdentifier { loudness }
						)] {
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testFunctionBody() {
		val sourceCode = "class Animal { fun getSound(loudness: Int) { var energy = loudness * 2 } }"
		val expected =
			"""
				Program {
					Class [ClassIdentifier { Animal }] {
						Function [FunctionIdentifier { getSound }(
							VariableIdentifier { loudness }
						)] {
							Declaration {
								Assignment { VariableIdentifier { energy } = Multiplication { VariableReference { loudness } * NumberLiteral { 2 } } }
							}
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}