package components.code_generation

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class ValueConversion {

	@Test
	fun `casts primitive integers to floats`() {
		val sourceCode = """
			SimplestApp object {
				val a = 19
				val b: Float = a
				to getNineteen(): Float {
					return b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNineteen")
		assertEquals(19.0, Llvm.castToFloat(result))
	}

	@Test
	fun `casts primitive bytes to integers`() {
		val sourceCode = """
			SimplestApp object {
				val a: Byte = 51
				val b: Int = a
				to getFiftyOne(): Int {
					return b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFiftyOne")
		assertEquals(51, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `casts primitive bytes to floats`() {
		val sourceCode = """
			SimplestApp object {
				val a: Byte = 71
				val b: Float = a
				to getSeventyOne(): Float {
					return b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSeventyOne")
		assertEquals(71.0, Llvm.castToFloat(result))
	}

	@Test
	fun `implicitly converts between types`() {
		val sourceCode = """
			Inches class {
				val value: Float
				init(value)
			}
			Meters class {
				val value: Float
				converting init(inches: Inches) {
					value = inches.value * 0.0254
				}
			}
			SimplestApp object {
				to getRawMeters(meters: Meters): Float {
					return meters.value
				}
				to getFive(): Float {
					val inches = Inches(196.85)
					return getRawMeters(inches)
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result), 0.01)
	}
}
