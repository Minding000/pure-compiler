package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

internal class ForeignLanguageExpressions {
	//TODO: parse foreign language expression using external parser which dictates the expression end

	@Test
	fun `parses regular expression example`() {
		val sourceCode = """
			RegExp::/^hello .*!/
		""".trimIndent()
		val expected =
			"""
				ForeignLanguageExpression [ Identifier { RegExp } ] { ForeignLanguageLiteral { /^hello .*!/ } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses JSON example`() {
		val sourceCode = """
			Json::{status: "GREAT"}
		""".trimIndent()
		val expected =
			"""
				ForeignLanguageExpression [ Identifier { Json } ] { ForeignLanguageLiteral { {status: "GREAT"} } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses color example`() {
		val sourceCode = """
			Color::ff22c8
			Color::rgba(99, 3, 4)
			Color::hsl(45, 123, 233)
		""".trimIndent()
		val expected =
			"""
				ForeignLanguageExpression [ Identifier { Color } ] { ForeignLanguageLiteral { ff22c8 } }
				ForeignLanguageExpression [ Identifier { Color } ] { ForeignLanguageLiteral { rgba(99, 3, 4) } }
				ForeignLanguageExpression [ Identifier { Color } ] { ForeignLanguageLiteral { hsl(45, 123, 233) } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses HTML example`() {
		val sourceCode = """
			Html::<div></div>
		""".trimIndent()
		val expected =
			"""
				ForeignLanguageExpression [ Identifier { Html } ] { ForeignLanguageLiteral { <div></div> } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
