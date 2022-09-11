package parsing

import util.TestUtil
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class AwaitTest {

	@Disabled
	@Test
	fun testAwaitSingle() {
		val sourceCode = """
			let data = await fetchData()
			""".trimIndent()
		val expected =
			"""
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Disabled
	@Test
	fun testAwaitMultiple() {
		val sourceCode = """
			let (config, data) = await (
				loadConfig(),
				fetchData()
			)
			""".trimIndent()
		val expected =
			"""
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Disabled
	@Test
	fun testCompleteMultiple() {
		val sourceCode = """
			let (configResult, dataResult) = await all {
				loadConfig()
				fetchData()
			}
			""".trimIndent()
		val expected =
			"""
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Disabled
	@Test
	fun testCompleteMultipleOptional() {
		val sourceCode = """
			let (configResult, dataResult) = await all? {
				loadConfig()
				fetchData()
			}
			""".trimIndent()
		val expected =
			"""
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Disabled
	@Test
	fun testAnyMultiple() {
		val sourceCode = """
			any {
				let config = async loadConfig()
				let data = async fetchData()
			}
			""".trimIndent()
		val expected =
			"""
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}