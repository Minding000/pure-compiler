package components.code_generation.operators

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class BinaryBooleanOperators {

	@Test
	fun `compiles boolean and`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return yes & no
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles boolean or`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					return yes | no
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles boolean equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return yes == no
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles boolean not equal to`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					return yes != no
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, Llvm.castToBoolean(result))
	}
}
