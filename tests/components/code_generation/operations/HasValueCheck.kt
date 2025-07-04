package components.code_generation.operations

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class HasValueCheck {

	@Test
	fun `compiles when value is null literal`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return null?
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, result)
	}

	@Test
	fun `compiles when value is non-optional`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					return 3?
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, result)
	}

	@Test
	fun `compiles optional variable with value as subject`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					val app: SimplestApp? = SimplestApp
					return app?
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, result)
	}

	@Test
	fun `compiles optional variable without value as subject`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					val app: SimplestApp? = null
					return app?
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, result)
	}
}
