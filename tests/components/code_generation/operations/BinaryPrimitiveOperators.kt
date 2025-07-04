package components.code_generation.operations

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class BinaryPrimitiveOperators {

	@Test
	fun `compiles with primitive generic properties on left hand side`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			SimplestApp object {
				to getTwentyFour(): Int {
					val container = Container(8)
					return container.a * 3
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTwentyFour")
		assertEquals(24, result)
	}

	@Test
	fun `compiles with primitive generic properties on right hand side`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			SimplestApp object {
				to getThree(): Int {
					var b = 9
					val container = Container(3)
					return b / container.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThree")
		assertEquals(3, result)
	}
}
