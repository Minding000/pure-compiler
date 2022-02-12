package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class ForeignLanguageTest {
	//TODO: parse foreign language expression using external parser which dictates the expression end

	@Test
	fun testRegularExpression() {
		val sourceCode = """
			RegExp::/^hello .*!/
		""".trimIndent()
		val expected =
			"""
				ForeignLanguageExpression [ Identifier { RegExp } ] { ForeignLanguageLiteral { /^hello .*!/ } }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testJson() {
		val sourceCode = """
			Json::{status: "GREAT"}
		""".trimIndent()
		val expected =
			"""
				ForeignLanguageExpression [ Identifier { Json } ] { ForeignLanguageLiteral { {status: "GREAT"} } }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testColor() {
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
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testHTML() {
		val sourceCode = """
			Html::<div></div>
		""".trimIndent()
		val expected =
			"""
				ForeignLanguageExpression [ Identifier { Html } ] { ForeignLanguageLiteral { <div></div> } }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}