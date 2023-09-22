package components.code_generation.operators

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class BinaryModifications {

	@Test
	fun `compiles integer addition assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var a = 3
					a += 2
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles integer subtraction assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var a = 8
					a -= 3
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles integer multiplication assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var a = 1
					a *= 5
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles integer division assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var a = 20
					a /= 4
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles float addition assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 2.3
					a += 2.7
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles float subtraction assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 9.5
					a -= 4.5
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles float multiplication assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 2.5
					a *= 2.0
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles float division assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 7.5
					a /= 1.5
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles addition assignments with float target and integer modifier`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 2.0
					a += 3
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles subtraction assignments with float target and integer modifier`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 7.0
					a -= 2
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles multiplication assignments with float target and integer modifier`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 2.5
					a *= 2
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles division assignments with float target and integer modifier`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					var a = 10.0
					a /= 2
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles custom operator calls`() {
		val sourceCode = """
			Pool class {
				var subPoolCount = 0
				operator +=(other: Pool) {
					subPoolCount++
				}
			}
			SimplestApp object {
				to getOne(): Int {
					val pool = Pool()
					pool += Pool()
					return pool.subPoolCount
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, Llvm.castToSignedInteger(result))
	}
}
