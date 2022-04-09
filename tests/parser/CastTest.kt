package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class CastTest {

	@Test
	fun testSafeCast() {
		val sourceCode = "10 as Float"
		val expected =
			"""
				Cast {
					NumberLiteral { 10 } as SimpleType { Identifier { Float } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testOptionalCast() {
		val sourceCode = "quoteSource as? Book"
		val expected =
			"""
				Cast {
					Identifier { quoteSource } as? SimpleType { Identifier { Book } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testForceCast() {
		val sourceCode = "food as! Fruit"
		val expected =
			"""
				Cast {
					Identifier { food } as! SimpleType { Identifier { Fruit } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testIs() {
		val sourceCode =
			"""
				if inputDevice is keyboard: Keyboard {
				}
            """.trimIndent()
		val expected =
			"""
				If [ Cast {
					Identifier { inputDevice } is TypedIdentifier { Identifier { keyboard }: SimpleType { Identifier { Keyboard } } }
				} ] {
					StatementSection { StatementBlock {
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testIsNot() {
		val sourceCode =
			"""
				if inputDevice is! keyboard: Keyboard {
				}
            """.trimIndent()
		val expected =
			"""
				If [ Cast {
					Identifier { inputDevice } is! TypedIdentifier { Identifier { keyboard }: SimpleType { Identifier { Keyboard } } }
				} ] {
					StatementSection { StatementBlock {
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}