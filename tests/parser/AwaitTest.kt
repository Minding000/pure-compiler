package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class AwaitTest {

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