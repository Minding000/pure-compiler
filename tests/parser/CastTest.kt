package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class CastTest {

	@Test
	fun testSafeCast() {
		val sourceCode = "10 as Float"
		val expected =
			"""
				Program {
					Cast {
						NumberLiteral { 10 } as Type { Identifier { Float } }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testOptionalCast() {
		val sourceCode = "quoteSource as? Book"
		val expected =
			"""
				Program {
					Cast {
						Identifier { quoteSource } as? Type { Identifier { Book } }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testForceCast() {
		val sourceCode = "food as! Fruit"
		val expected =
			"""
				Program {
					Cast {
						Identifier { food } as! Type { Identifier { Fruit } }
					}
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
				Program {
					If [ Cast {
						Identifier { inputDevice } is TypedIdentifier { Identifier { keyboard } : Type { Identifier { Keyboard } } }
					} ] {
						StatementBlock {
						}
					}
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
				Program {
					If [ Cast {
						Identifier { inputDevice } is! TypedIdentifier { Identifier { keyboard } : Type { Identifier { Keyboard } } }
					} ] {
						StatementBlock {
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}