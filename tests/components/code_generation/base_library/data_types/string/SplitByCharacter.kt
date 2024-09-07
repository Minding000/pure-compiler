package components.code_generation.base_library.data_types.string

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class SplitByCharacter {

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
