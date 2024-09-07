package components.code_generation.base_library.collections.primitive_arrays.byte_array

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class CreateIterator {

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
}
