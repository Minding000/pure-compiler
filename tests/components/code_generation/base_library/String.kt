package components.code_generation.base_library

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class String {

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

	@Test
	fun `determines equality based on characters`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getNine(): Int {
					val aBytes = <Byte>List()
					aBytes.append(48)
					aBytes.append(50)
					val a = String(aBytes)
					val b = "02"
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
