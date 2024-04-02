package components.code_generation.operations

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class BinaryFloatOperators {

	@Test
	fun `compiles float additions`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					return 4.2 + 0.8
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles float subtractions`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					return 7.3 - 2.3
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles float multiplications`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					return 4.0 * 1.25
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles float divisions`() {
		val sourceCode = """
			SimplestApp object {
				to getTwoPointFive(): Float {
					return 7.5 / 3.0
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTwoPointFive")
		assertEquals(2.5, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles float smaller than`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return 2.3 < 2.3
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles float greater than`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return 2.3 > 2.3
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles float smaller than or equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					return 2.3 <= 2.3
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles float greater than or equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					return 2.3 >= 2.3
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles float equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					return 2.3 == 2.3
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles float not equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return 2.3 != 2.3
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, Llvm.castToBoolean(result))
	}
}
