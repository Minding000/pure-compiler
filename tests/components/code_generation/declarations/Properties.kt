package components.code_generation.declarations

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Properties {

	@Test
	fun `compiles with primitive generic properties`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			SimplestApp object {
				val container = Container(16)
				val b = container.a
				to getSixteen(): Int {
					return b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSixteen")
		assertEquals(16, Llvm.castToSignedInteger(result))
	}
}
