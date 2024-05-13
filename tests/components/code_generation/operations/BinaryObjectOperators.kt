package components.code_generation.operations

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class BinaryObjectOperators {

	@Test
	fun `compiles custom operator calls`() {
		val sourceCode = """
			Joker class {
				operator ==(other: Joker): Bool {
					return yes
				}
			}
			SimplestApp object {
				to getYes(): Bool {
					return Joker() == Joker()
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, result)
	}
}
