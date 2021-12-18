import org.junit.jupiter.api.Test

internal class FunctionDefinitionTest {

	@Test
	fun testFunctionDeclaration() {
		val sourceCode = "class Animal { fun getSound(loudness: Int) {  } }"
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Animal }] {
						TypeBody {
							Function [Identifier { getSound }(
								TypedIdentifier { Identifier { loudness } : Type { Identifier { Int } } }
							): void] {
							}
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
					TypeDefinition [TypeType { class } Identifier { Animal }] {
						TypeBody {
							Function [Identifier { getSound }(
								TypedIdentifier { Identifier { loudness } : Type { Identifier { Int } } }
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
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}