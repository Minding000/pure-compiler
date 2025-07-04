package components.code_generation.general

import components.semantic_model.context.SpecialType
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
		val result = TestUtil.runAndReturnFloat(sourceCode, "Test:SimplestApp.getNineteen")
		assertEquals(19.0, result)
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
		assertEquals(51, result)
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
		val result = TestUtil.runAndReturnFloat(sourceCode, "Test:SimplestApp.getSeventyOne")
		assertEquals(71.0, result)
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
		val result = TestUtil.runAndReturnFloat(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, result, 0.01)
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
		assertEquals(99, result)
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
		assertEquals(99, result)
	}

	@Test
	fun `doesn't convert between wrapped primitive source and wrapped primitive target`() {
		val sourceCode = """
			SimplestApp object {
				val source: Number = 99
				val target: Number = source
				to getNinetyNine(): Int {
					return target as! Int
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine")
		assertEquals(99, result)
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
		assertEquals(99, result)
	}

	@Test
	fun `implicitly converts from primitive to wrapped primitive`() {
		val sourceCode = """
			SimplestApp object {
				val source = 99
				val target: Number = source
				to getNinetyNine(): Int {
					return target as! Int
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine")
		assertEquals(99, result)
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
		assertEquals(99, result)
	}

	@Test
	fun `implicitly converts from boxed primitive to wrapped primitive`() {
		val sourceCode = """
			SimplestApp object {
				val source: Int? = 99
				val target: Number? = source
				to getNinetyNine(): Int {
					return target as! Int
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine")
		assertEquals(99, result)
	}

	@Test
	fun `implicitly converts from wrapped primitive to primitive`() {
		val sourceCode = """
			SimplestApp object {
				val source: Number = 99
				val target = source as! Int
				to getNinetyNine(): Int {
					return target
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine")
		assertEquals(99, result)
	}

	@Test
	fun `implicitly converts from wrapped primitive to boxed primitive`() {
		val sourceCode = """
			SimplestApp object {
				val source: Number? = 99
				val target = source as! Int?
				to getNinetyNine(): Int {
					return target as! Int
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine")
		assertEquals(99, result)
	}

	@Test
	fun `implicitly converts function call parameter`() {
		val sourceCode = """
			native copied Int class {
				native converting init(value: Byte)
				native init(value: Int)
			}
			SimplestApp object {
				val source: Byte = 94
				to plusFive(target: Int): Int {
					return target + 5
				}
				to getNinetyNine(): Int {
					return plusFive(source)
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyNine", listOf(SpecialType.INTEGER))
		assertEquals(99, result)
	}

	@Test
	fun `implicitly converts generic function return type`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
				to getA(): Item {
					return a
				}
			}
			SimplestApp object {
				to getEightyFive(): Int {
					val container = Container(85)
					return container.getA()
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightyFive")
		assertEquals(85, result)
	}

	@Test
	fun `implicitly converts generic binary operator return type`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
				operator +(b: Int): Item {
					return a
				}
			}
			SimplestApp object {
				to getEightyFive(): Int {
					val container = Container(85)
					return container + 0
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightyFive")
		assertEquals(85, result)
	}

	@Test
	fun `implicitly converts generic unary operator return type`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
				operator -: Item {
					return a
				}
			}
			SimplestApp object {
				to getEightyFive(): Int {
					val container = Container(85)
					return -container
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightyFive")
		assertEquals(85, result)
	}

	@Test
	fun `implicitly converts generic index access return type`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
				operator [b: Int]: Item {
					return a
				}
			}
			SimplestApp object {
				to getEightyFive(): Int {
					val container = Container(85)
					return container[0]
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightyFive")
		assertEquals(85, result)
	}

	@Test
	fun `converts primitives to non-wrapper objects`() {
		val sourceCode = """
			NonWrapper class {
				val b: Int
				converting init(b)
			}
			Container class {
				var a: NonWrapper
				init(a)
			}
			SimplestApp object {
				to getEightyFive(): Int {
					val container = Container(85)
					return container.a.b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightyFive")
		assertEquals(85, result)
	}

	@Test
	fun `converts generic primitives used in primitive conversion`() {
		val sourceCode = """
			Container class {
				containing N
				var a: N
				init(a)
			}
			Target class {
				val b: Int
				converting init(b)
			}
			SimplestApp object {
				to getNinetyTwo(): Int {
					val container = Container(92)
					return getNumber(container.a)
				}
				to getNumber(target: Target): Int {
					return target.b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyTwo")
		assertEquals(92, result)
	}
}
