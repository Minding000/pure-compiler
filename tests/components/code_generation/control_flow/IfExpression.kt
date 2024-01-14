package components.code_generation.control_flow

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class IfExpression {

	@Test
	fun `compiles if statements without negative branch`() {
		val sourceCode = """
			SimplestApp object {
				to getFiveOrTen(): Int {
					if yes
						return 10
					return 5
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFiveOrTen")
		assertEquals(10, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles if statements with negative branch`() {
		val sourceCode = """
			SimplestApp object {
				to getTenOrTwelve(): Int {
					if no
						return 10
					else
						return 12
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTenOrTwelve")
		assertEquals(12, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles if expressions that don't interrupt execution`() {
		val sourceCode = """
			SimplestApp object {
				to getTenOrTwelve(): Int {
					return if yes 10 else 12
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTenOrTwelve")
		assertEquals(10, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles if expressions that may interrupt execution`() {
		val sourceCode = """
			SimplestApp object {
				to getTenOrFortyOne(): Int {
					return if no 10 else return 41
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTenOrFortyOne")
		assertEquals(41, Llvm.castToSignedInteger(result))
	}
}
