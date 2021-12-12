import org.junit.jupiter.api.Test

internal class ClassDefinitionTest {

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
}