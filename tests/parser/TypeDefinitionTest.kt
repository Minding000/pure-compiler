package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class TypeDefinitionTest {

	@Test
	fun testClassDeclaration() {
		val sourceCode = "class Animal {}"
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Animal }] { TypeBody {
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testObjectDeclaration() {
		val sourceCode = "object Dog {}"
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { object } Identifier { Dog }] { TypeBody {
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testGenericDeclaration() {
		val sourceCode = "generic Barrel {}"
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { generic } Identifier { Barrel }] { TypeBody {
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testNativeDeclaration() {
		val sourceCode = "native class Goldfish {}"
		val expected =
			"""
				Program {
					TypeDefinition [ModifierList { Modifier { native } } TypeType { class } Identifier { Goldfish }] { TypeBody {
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}