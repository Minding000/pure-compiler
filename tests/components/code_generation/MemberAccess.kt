package components.code_generation

import components.code_generation.llvm.Llvm
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

	@Test
	fun `compiles calls to overridden functions`() {
		val sourceCode = """
			Application class {
				to getId(): Int {
					return 1
				}
				to getKey(): Int {
					return getId()
				}
			}
			SimplestApp object: Application {
				overriding to getId(): Int {
					return 2
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getKey")
		assertEquals(2, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles calls to super functions`() {
		val sourceCode = """
			Application class {
				to getId(): Int {
					return 1
				}
				to getKey(): Int {
					return getId()
				}
			}
			SimplestApp object: Application {
				overriding to getId(): Int {
					return 2 + super.getId()
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getKey")
		assertEquals(3, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles optional member access with target value`() {
		val sourceCode = """
			SimplestApp object {
				val a: Int = 62
				to getA(): Int {
					val app: SimplestApp? = SimplestApp
					return app?.a ?? 0
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(62, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles optional member access without target value`() {
		val sourceCode = """
			SimplestApp object {
				val a: Int = 62
				to getA(): Int {
					val app: SimplestApp? = null
					return app?.a ?? 0
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(0, Llvm.castToSignedInteger(result))
	}
}
