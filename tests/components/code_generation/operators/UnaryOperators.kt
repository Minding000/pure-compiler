package components.code_generation.operators

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

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
}
