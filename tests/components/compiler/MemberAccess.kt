package components.compiler

import components.compiler.targets.llvm.Llvm
import org.junit.jupiter.api.Disabled
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

	//TODO write super initializer tests
	// - default initializer
	// - custom initializer
	//TODO write function resolution test

	@Test
	fun `compiles explicit member access to super class`() {
		val sourceCode = """
			Application class {
				val id = 3
			}
			SimplestApp object: Application {
				val a = 62
				to getId(): Int {
					return SimplestApp.id
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getId")
		assertEquals(3, Llvm.castToSignedInteger(result))
	}

	@Disabled
	@Test
	fun `compiles implicit member access to super class`() {
		val sourceCode = """
			Application class {
				val id = 3
			}
			SimplestApp object: Application {
				val a = 62
				to getId(): Int {
					return id
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getId")
		assertEquals(3, Llvm.castToSignedInteger(result))
	}
}
