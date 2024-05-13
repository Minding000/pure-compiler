package components.code_generation.declarations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Primitives {

	@Test
	fun `compiles native primitive operator`() {
		val sourceCode = """
			native copied Int class {
				native init(value: Int)
				native operator ++
			}
			SimplestApp object {
				to getNinetyFive(): Int {
					var a = 94
					a++
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyFive", mapOf(
			SpecialType.INTEGER to TestUtil.TEST_FILE_NAME
		))
		assertEquals(95, result)
	}

	@Test
	fun `compiles native primitive function`() {
		val sourceCode = """
			native copied Int class {
				native init(value: Int)
				native it toThePowerOf(exponent: Int): Int
			}
			SimplestApp object {
				to getThirtyTwo(): Int {
					return 2.toThePowerOf(5)
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThirtyTwo", mapOf(
			SpecialType.INTEGER to TestUtil.TEST_FILE_NAME
		))
		assertEquals(32, result)
	}

	@Test
	fun `compiles native primitive converting initializer`() {
		val sourceCode = """
			native copied Int class {
				native converting init(value: Byte)
				native init(value: Int)
			}
			SimplestApp object {
				to getSeventyFour(): Int {
					val byte = 74
					return Int(byte)
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSeventyFour", mapOf(
			SpecialType.INTEGER to TestUtil.TEST_FILE_NAME
		))
		assertEquals(74, result)
	}

	@Test
	fun `compiles instances in primitive class`() {
		val sourceCode = """
			native copied Int class {
				overriding instances ZERO(0), ONE(1)
				native init(value: Int)
			}
			SimplestApp object {
				to getOne(): Int {
					return Int.ONE
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne", mapOf(
			SpecialType.INTEGER to TestUtil.TEST_FILE_NAME
		))
		assertEquals(1, result)
	}
}
