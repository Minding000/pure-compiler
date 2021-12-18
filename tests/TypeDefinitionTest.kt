import org.junit.jupiter.api.Test

internal class TypeDefinitionTest {

	@Test
	fun testClassDeclaration() {
		val sourceCode = "class Animal {}"
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Animal }] {
						TypeBody {
						}
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
					TypeDefinition [TypeType { object } Identifier { Animal }] {
						TypeBody {
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}