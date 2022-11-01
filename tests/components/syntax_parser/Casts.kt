package components.syntax_parser

import util.TestUtil
import org.junit.jupiter.api.Test

internal class Casts {

	@Test
	fun `parses safe casts`() {
		val sourceCode = "10 as Float"
		val expected =
			"""
				Cast {
					NumberLiteral { 10 } as ObjectType { Identifier { Float } }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses optional casts`() {
		val sourceCode = "quoteSource as? Book"
		val expected =
			"""
				Cast {
					Identifier { quoteSource } as? ObjectType { Identifier { Book } }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses force casts`() {
		val sourceCode = "food as! Fruit"
		val expected =
			"""
				Cast {
					Identifier { food } as! ObjectType { Identifier { Fruit } }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses type checks`() {
		val sourceCode =
			"""
				if inputDevice is keyboard: Keyboard {
				}
            """.trimIndent()
		val expected =
			"""
				If [ Cast {
					Identifier { inputDevice } is Identifier { keyboard }: ObjectType { Identifier { Keyboard } }
				} ] {
					StatementSection { StatementBlock {
					} }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses negated type checks`() {
		val sourceCode =
			"""
				if inputDevice is! keyboard: Keyboard {
				}
            """.trimIndent()
		val expected =
			"""
				If [ Cast {
					Identifier { inputDevice } is! Identifier { keyboard }: ObjectType { Identifier { Keyboard } }
				} ] {
					StatementSection { StatementBlock {
					} }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
