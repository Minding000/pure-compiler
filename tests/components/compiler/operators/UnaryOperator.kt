package components.compiler.operators

import components.compiler.targets.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class UnaryOperator {

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
		assertEquals(false, Llvm.castToBool(result))
	}

	@Test
	fun `compiles integer negate`() {
		val sourceCode = """
			SimplestApp object {
				to getNegativeOne(): Int {
					return -1
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNegativeOne")
		assertEquals(-1, Llvm.castToSignedInt(result))
	}
}
