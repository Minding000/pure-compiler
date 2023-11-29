package components.syntax_parser

import logger.Severity
import logger.issues.parsing.UnexpectedEndOfFile
import logger.issues.tokenization.UnknownWord
import org.junit.jupiter.api.Test
import util.TestUtil

internal class General {

	@Test
	fun `ignores empty files`() {
		TestUtil.assertSyntaxTreeEquals("", "")
	}

	@Test
	fun `emits error for unexpected end of file`() {
		val sourceCode = """
			!
			""".trimIndent()
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertIssueDetected<UnexpectedEndOfFile>("""
			Unexpected end of file 'Test.Test'.
			Expected atom instead.
		""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `emits error for unknown words`() {
		val sourceCode = """
			↓
			""".trimIndent()
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertIssueDetected<UnknownWord>("""
			Unknown word in Test:1:0: '↓'.
		""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `recovers after unknown words`() {
		val sourceCode = """
			☻
			player.name
			""".trimIndent()
		val expected =
			"""
				MemberAccess {
					Identifier { player }.Identifier { name }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `recovers after syntax error in file`() {
		val sourceCode = """
			++
			player.name
			""".trimIndent()
		val expected =
			"""
				MemberAccess {
					Identifier { player }.Identifier { name }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `recovers after syntax error in statement block`() {
		val sourceCode = """
			{
				++
				player.name
			}
			""".trimIndent()
		val expected =
			"""
				StatementSection { StatementBlock {
					MemberAccess {
						Identifier { player }.Identifier { name }
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `recovers after syntax error in type definition`() {
		val sourceCode = """
			Volcano class {
				va height = Int
				val name: String
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Volcano } class ] { TypeBody {
					VariableSection [ val ] {
						PropertyDeclaration { Identifier { name }: ObjectType { Identifier { String } } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
