package components.syntax_parser

import messages.Message
import util.TestUtil
import org.junit.jupiter.api.Test

internal class UnaryOperators {

	@Test
	fun `parses negations`() {
		val sourceCode = "!yes"
		val expected =
			"""
				UnaryOperator { !BooleanLiteral { yes } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses positive signs`() {
		val sourceCode = "+2"
		val expected =
			"""
				UnaryOperator { +NumberLiteral { 2 } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses negative signs`() {
		val sourceCode = "-6"
		val expected =
			"""
				UnaryOperator { -NumberLiteral { 6 } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses spread operators`() {
		val sourceCode =
			"""
				sum(...numbers)
            """.trimIndent()
		val expected =
			"""
				FunctionCall [ Identifier { sum } ] {
					UnaryOperator { ...Identifier { numbers } }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses null checks`() {
		val sourceCode = "x?"
		val expected =
			"""
				NullCheck { Identifier { x } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `emits error for multiple negations`() {
		val sourceCode = "!!yes"
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertMessageEmitted(Message.Type.ERROR, "Unexpected NOT")
	}

	@Test
	fun `emits error for multiple negative signs`() {
		val sourceCode = "--4"
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertMessageEmitted(Message.Type.ERROR, "Unexpected DECREMENT")
	}

	@Test
	fun `emits error for multiple positive signs`() {
		val sourceCode = "++8"
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertMessageEmitted(Message.Type.ERROR, "Unexpected INCREMENT")
	}
}
