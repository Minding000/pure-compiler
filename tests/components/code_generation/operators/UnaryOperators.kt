package components.code_generation.operators

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class UnaryOperators {

	@Test
	fun `compiles boolean not`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return !yes
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles byte negation`() {
		val sourceCode = """
			SimplestApp object {
				to getNegativeOne(): Byte {
					val byte: Byte = 1
					return -byte
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNegativeOne")
		assertEquals(-1, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles integer negation`() {
		val sourceCode = """
			SimplestApp object {
				to getNegativeOne(): Int {
					return -1
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNegativeOne")
		assertEquals(-1, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles float negation`() {
		val sourceCode = """
			SimplestApp object {
				to getNegativeOnePointFive(): Float {
					return -1.5
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNegativeOnePointFive")
		assertEquals(-1.5, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles custom operator calls`() {
		val sourceCode = """
			Acceleration class {
				var value = 0
				operator -(): Acceleration {
					val negativeAcceleration = Acceleration()
					negativeAcceleration.value = -value
					return negativeAcceleration
				}
			}
			SimplestApp object {
				to getNegativeTen(): Int {
					val acceleration = Acceleration()
					acceleration.value = 10
					val negativeAcceleration = -acceleration
					return negativeAcceleration.value
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNegativeTen")
		assertEquals(-10, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles with primitive generic properties`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			SimplestApp object {
				to getNo(): Bool {
					val container = Container(yes)
					return !container.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNo")
		assertFalse(Llvm.castToBoolean(result))
	}
}
