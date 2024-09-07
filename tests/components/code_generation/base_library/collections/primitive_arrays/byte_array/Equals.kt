package components.code_generation.base_library.collections.primitive_arrays.byte_array

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Equals {

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
