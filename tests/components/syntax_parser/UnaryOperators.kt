package components.syntax_parser

import logger.Severity
import logger.issues.parsing.UnexpectedWord
import org.junit.jupiter.api.Test
import util.TestUtil

internal class UnaryOperators {

	@Test
	fun `parses negations`() {
		val sourceCode = "!yes"
		val expected =
			"""
				UnaryOperator { Operator { ! } BooleanLiteral { yes } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses positive signs`() {
		val sourceCode = "+2"
		val expected =
			"""
				UnaryOperator { Operator { + } NumberLiteral { 2 } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses negative signs`() {
		val sourceCode = "-6"
		val expected =
			"""
				UnaryOperator { Operator { - } NumberLiteral { 6 } }
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
					UnaryOperator { Operator { ... } Identifier { numbers } }
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
		parseResult.assertIssueDetected<UnexpectedWord>("""
			Unexpected NOT in Test.Test:1:1: '!'.
			!!yes
			 ^
			Expected atom instead.
		""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `emits error for multiple negative signs`() {
		val sourceCode = "--4"
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertIssueDetected<UnexpectedWord>("""
			Unexpected DECREMENT in Test.Test:1:0: '--'.
			--4
			^^
			Expected atom instead.
		""".trimIndent())
	}

	@Test
	fun `emits error for multiple positive signs`() {
		val sourceCode = "++8"
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertIssueDetected<UnexpectedWord>("""
			Unexpected INCREMENT in Test.Test:1:0: '++'.
			++8
			^^
			Expected atom instead.
		""".trimIndent())
	}
}
