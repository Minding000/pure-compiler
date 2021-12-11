import org.junit.jupiter.api.Test

internal class ClassDefinitionTest {

	@Test
	fun testClassDeclaration() {
		val sourceCode = "class Animal {}"
		val expected =
			"""
				Program {
					Class [ClassIdentifier { Animal }] {
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}