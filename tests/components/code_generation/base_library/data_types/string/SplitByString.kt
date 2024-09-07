package components.code_generation.base_library.data_types.string

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class SplitByString {

	@Test
	fun `can be split using a string delimiter`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getThree(): Int {
					val digits = "1,2,3".split(",")
					return digits.size
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThree", true)
		assertEquals(3, result)
	}
}
