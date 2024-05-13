package components.code_generation.declarations

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class LocalVariables {

	@Test
	fun `compiles with primitive generic properties`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			SimplestApp object {
				to getSixteen(): Int {
					val container = Container(16)
					val b = container.a
					return b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSixteen")
		assertEquals(16, result)
	}
}
