package components.code_generation.operations

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class NullCoalescenceOperator {

	@Test
	fun `compiles null coalescence with value`() {
		val sourceCode = """
			SimplestApp object {
				to getEight(): Int {
					return 8 ?? 3
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEight")
		assertEquals(8, result)
	}

	@Test
	fun `compiles null coalescence without value`() {
		val sourceCode = """
			SimplestApp object {
				to getThree(): Int {
					return null ?? 3
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThree")
		assertEquals(3, result)
	}

	@Test
	fun `unboxes primitive left operand when result type is non-optional`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					val score: Int? = 5
					return score ?? 4
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `doesn't unbox primitive left operand when result type is optional`() {
		val sourceCode = """
			SimplestApp object {
				to getSeven(): Int {
					val highScore: Int? = 7
					val currentScore: Int? = 4
					val score = highScore ?? currentScore
					return score ?? 9
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSeven")
		assertEquals(7, result)
	}
}
