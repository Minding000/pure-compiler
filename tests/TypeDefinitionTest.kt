import org.junit.jupiter.api.Test

internal class TypeDefinitionTest {

	@Test
	fun testClassDeclaration() {
		val sourceCode = "class Animal {}"
		val expected =
			"""
				Program {
					Class [Identifier { Animal }] {
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testObjectDeclaration() {
		val sourceCode = "object Animal {}"
		val expected =
			"""
				Program {
					Object [Identifier { Animal }] {
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}