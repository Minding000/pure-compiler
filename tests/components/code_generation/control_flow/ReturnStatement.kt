package components.code_generation.control_flow

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class ReturnStatement {

	@Test
	fun `compiles with primitive generic property`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			SimplestApp object {
				to getFourteen(): Int {
					val container = Container(14)
					return container.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFourteen")
		assertEquals(14, result)
	}
}
