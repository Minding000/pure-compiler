package components.code_generation.operators

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class IndexAccess {

	@Test
	fun `compiles custom get operator`() {
		val sourceCode = """
			Mirror object {
				operator [index: Int]: Int {
					return index
				}
			}
			SimplestApp object {
				to getTwelve(): Int {
					return Mirror[12]
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTwelve")
		assertEquals(12, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles custom set operator`() {
		val sourceCode = """
			SingletonList object {
				var value = 0
				operator [index: Int](newValue: Int) {
					value = newValue
				}
			}
			SimplestApp object {
				to getTwo(): Int {
					SingletonList[0] = 2
					return SingletonList.value
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTwo")
		assertEquals(2, Llvm.castToSignedInteger(result))
	}
}
