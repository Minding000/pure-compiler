package components.code_generation.operators

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class UnaryModifications {

	@Test
	fun `compiles integer increments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var a = 4
					a++
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles integer decrements`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var a = 6
					a--
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}
}
