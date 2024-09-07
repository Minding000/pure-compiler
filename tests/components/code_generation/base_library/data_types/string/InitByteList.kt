package components.code_generation.base_library.data_types.string

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class InitByteList {

	@Test
	fun `can be constructed from a byte list`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getTwo(): Int {
					val bytes = <Byte>List()
					bytes.append(48)
					bytes.append(65)
					return String(bytes).byteCount
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTwo", true)
		assertEquals(2, result)
	}
}
