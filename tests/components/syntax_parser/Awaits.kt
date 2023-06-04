package components.syntax_parser

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Awaits {

	@Disabled
	@Test
	fun `parses awaits with a single value`() {
		val sourceCode = """
			let data = await fetchData()
			""".trimIndent()
		val expected =
			"""
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Disabled
	@Test
	fun `parses awaits with multiple values`() {
		val sourceCode = """
			let (config, data) = await (
				loadConfig(),
				fetchData()
			)
			""".trimIndent()
		val expected =
			"""
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Disabled
	@Test
	fun `parses awaits with modifier`() {
		val sourceCode = """
			let (configResult, dataResult) = await all {
				loadConfig()
				fetchData()
			}
			""".trimIndent()
		val expected =
			"""
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Disabled
	@Test
	fun `parses awaits with optional modifier`() {
		val sourceCode = """
			let (configResult, dataResult) = await all? {
				loadConfig()
				fetchData()
			}
			""".trimIndent()
		val expected =
			"""
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Disabled
	@Test
	fun `parses awaits for any of multiple values`() {
		val sourceCode = """
			any {
				let config = async loadConfig()
				let data = async fetchData()
			}
			""".trimIndent()
		val expected =
			"""
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
