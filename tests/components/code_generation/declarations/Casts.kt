package components.code_generation.declarations

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Casts {

	@Test
	fun `compiles conditional cast with declaration`() {
		val sourceCode = """
			NumberWrapper class
			IntWrapper class: NumberWrapper {
				val value: Int
				init(value)
			}
			SimplestApp object {
				to getB(): Int {
					val a: NumberWrapper = IntWrapper(84)
					if a is b: IntWrapper
						return b.value
					return 0
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getB")
		assertEquals(84, result)
	}

	@Test
	fun `compiles inverted conditional cast with declaration`() {
		val sourceCode = """
			NumberWrapper class
			IntWrapper class: NumberWrapper {
				val value: Int
				init(value)
			}
			SimplestApp object {
				to getB(): Int {
					val a: NumberWrapper = IntWrapper(124)
					if a is! b: IntWrapper
						return 0
					return b.value
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getB")
		assertEquals(124, result)
	}
}
