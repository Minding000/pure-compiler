package components.code_generation.declarations

import components.semantic_model.context.SpecialType
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
		assertEquals(16, result)
	}

	@Test
	fun `compiles properties in bound objects`() {
		val sourceCode = """
			SimplestApp object {
				bound Process object {
					val a = 58
				}
				to getFiftyEight(): Int {
					return Process.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFiftyEight")
		assertEquals(58, result)
	}

	@Test
	fun `compiles primitive type alias properties`() {
		val sourceCode = """
			native copied Int class {
				native init(value: Int)
			}
			alias ExitCode = Int {
				instances ERROR(1)
			}
			SimplestApp object {
				val exitCode: ExitCode = .ERROR
				to getOne(): Int {
					return exitCode
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne", listOf(SpecialType.INTEGER))
		assertEquals(1, result)
	}
}
