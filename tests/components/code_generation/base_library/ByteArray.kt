package components.code_generation.base_library

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class ByteArray {

	@Test
	fun `is iterable`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getSeventyFour(): Int {
					val bytes = ByteArray(10, -1, 65)
					var sum: Byte = 0
					loop over bytes as index, byte
						sum += byte
					return sum
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSeventyFour", true)
		assertEquals(74, result)
	}

	@Test
	fun `determines equality based on bytes`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getNine(): Int {
					val a = ByteArray(2, 32)
					val b = ByteArray(2, 32)
					if a == b
						return 9
					else
						return 2
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNine", true)
		assertEquals(9, result)
	}
}
