package components.code_generation.general

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class SelfReference {

	@Test
	fun `allows targeting bound enclosing class`() {
		val sourceCode = """
			Cookie class {
				to getNewId(): Int {
					return 38
				}
				bound Crumb class {
					var id = this<Cookie>.getNewId()
				}
			}
			SimplestApp object {
				to getA(): Int {
					val cookie = Cookie()
					return cookie.Crumb().id
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(38, Llvm.castToSignedInteger(result))
	}
}
