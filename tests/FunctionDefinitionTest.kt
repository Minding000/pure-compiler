import org.junit.jupiter.api.Test

internal class FunctionDefinitionTest {

	@Test
	fun testFunctionDeclaration() {
		val sourceCode = "class Animal { fun getSound(loudness: Int) {  } }"
		val expected =
			"""
				Program {
					Class [Identifier { Animal }] {
						Function [Identifier { getSound }(
							TypedIdentifier { Identifier { loudness } : Identifier { Int } }
						): void] {
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
					Class [Identifier { Animal }] {
						Function [Identifier { getSound }(
							TypedIdentifier { Identifier { loudness } : Identifier { Int } }
						): void] {
							Declaration {
								Assignment {
									Identifier { energy } = BinaryOperator {
										Identifier { loudness } * NumberLiteral { 2 }
									}
								}
							}
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}