package components.code_generation

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Initializer {

	@Test
	fun `compiles explicit super initializer calls`() {
		val sourceCode = """
			Application class {
				val a: Int
				init {
					a = 74
				}
			}
			SimplestApp object: Application {
				init {
					super.init()
				}
				to getA(): Int {
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(74, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `default initializer calls super initializer`() {
		val sourceCode = """
			Application class {
				val a: Int
				init {
					a = 74
				}
			}
			SimplestApp object: Application {
				to getA(): Int {
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(74, Llvm.castToSignedInteger(result))
	}
}
