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
				val source = 19
				val target: Float = source
				to getNineteen(): Float {
					return target
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
				val source: Byte = 51
				val target: Int = source
				to getFiftyOne(): Int {
					return target
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
				val source: Byte = 71
				val target: Float = source
				to getSeventyOne(): Float {
					return target
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

	@Test
	fun `doesn't convert between primitive source and primitive target`() {
		val sourceCode = """
			SimplestApp object {
				val source = 99
				val target: Int = source
				to getNinetyNine(): Int {
					return target
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine")
		assertEquals(99, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `doesn't convert between boxed primitive source and boxed primitive target`() {
		val sourceCode = """
			SimplestApp object {
				val source: Int? = 99
				val target: Int? = source
				to getNinetyNine(): Int {
					return target as! Int
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine")
		assertEquals(99, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `doesn't convert between wrapped primitive source and wrapped primitive target`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				val source: Number = 99
				val target: Number = source
				to getNinetyNine(): Int {
					return target as! Int
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine", true)
		assertEquals(99, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `implicitly converts from primitive to boxed primitive`() {
		val sourceCode = """
			SimplestApp object {
				val source = 99
				val target: Int? = source
				to getNinetyNine(): Int {
					return target as! Int
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine")
		assertEquals(99, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `implicitly converts from primitive to wrapped primitive`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				val source = 99
				val target: Number = source
				to getNinetyNine(): Int {
					return target as! Int
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine", true)
		assertEquals(99, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `implicitly converts from boxed primitive to primitive`() {
		val sourceCode = """
			SimplestApp object {
				val source: Int? = 99
				val target = source as! Int
				to getNinetyNine(): Int {
					return target
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine")
		assertEquals(99, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `implicitly converts from boxed primitive to wrapped primitive`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				val source: Int? = 99
				val target: Number? = source
				to getNinetyNine(): Int {
					return target as! Int
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine", true)
		assertEquals(99, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `implicitly converts from wrapped primitive to primitive`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				val source: Number = 99
				val target = source as! Int
				to getNinetyNine(): Int {
					return target
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine", true)
		assertEquals(99, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `implicitly converts from wrapped primitive to boxed primitive`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				val source: Number? = 99
				val target = source as! Int?
				to getNinetyNine(): Int {
					return target as! Int
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine", true)
		assertEquals(99, Llvm.castToSignedInteger(result))
	}
}
