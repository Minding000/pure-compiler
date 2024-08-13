package components.code_generation.base_library

import org.junit.jupiter.api.Test
import util.TestApp
import util.TestUtil
import kotlin.test.assertEquals

internal class Int {

	@Test
	fun `can be constructed from a string`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getFortySeven(): Int {
					return Int("47")
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFortySeven", true)
		assertEquals(47, result)
	}

	@Test
	fun `raises exception when constructing from a non-numeric string`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getFortySeven(): Int {
					return Int("0invalid")
				}
			}
		""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.getFortySeven")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: Invalid digit at index 1
			 at Pure:Int:17:Int(String)
			 at Test:Test:4:SimplestApp.getFortySeven(): Int
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}
}
