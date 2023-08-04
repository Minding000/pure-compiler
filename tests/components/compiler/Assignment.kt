package components.compiler

import components.compiler.targets.llvm.Llvm
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Assignment {

	@Test
	fun `compiles assignments to local variables`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					val five: Int
					five = 5
					return five
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles assignments to properties`() {
		val sourceCode = """
			SimplestApp object {
				var a = 11
				to getFive(): Int {
					a = 5
					return a
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Disabled
	@Test
	fun `compiles assignments to computed properties`() {
		val sourceCode = """
			SimplestApp object {
				var a = 12
				var b: Int
					gets a
					sets a = b
				to getFive(): Int {
					b = 5
					return a
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles assignments to member accesses`() {
		val sourceCode = """
			SimplestApp object {
				var a = 11
				to getFive(): Int {
					SimplestApp.a = 5
					return a
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles assignments to index accesses`() {
		val sourceCode = """
			SimplestApp object {
				var b = 1
				to getFive(): Int {
					this[0] = 5
					return b
				}
				operator[c: Int](a: Int) {
					b = a
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles assignments to overridden index accesses`() {
		val sourceCode = """
			Application class {
				var b = 1
				to getFive(): Int {
					this[0] = 5
					return b
				}
				operator[c: Int](a: Int) {
					b = 2
				}
			}
			SimplestApp object: Application {
				overriding operator[c: Int](a: Int) {
					b = a
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles assignments to super index accesses`() {
		val sourceCode = """
			Application class {
				var b = 1
				to getFive(): Int {
					this[0] = 5
					return b
				}
				operator[c: Int](a: Int) {
					b = 2
				}
			}
			SimplestApp object: Application {
				overriding operator[c: Int](a: Int) {
					super[c] = a
					b += 3
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}
}
