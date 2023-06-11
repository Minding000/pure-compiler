package components.compiler

import components.compiler.targets.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class MemberAccess {

	@Test
	fun `compiles member access`() {
		val sourceCode = """
			SimplestApp object {
				val a = 62
				to getA(): Int {
					return SimplestApp.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(62, Llvm.castToSignedInteger(result))
	}
}
