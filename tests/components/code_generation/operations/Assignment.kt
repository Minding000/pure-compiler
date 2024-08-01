package components.code_generation.operations

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
		assertEquals(5, result)
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
		assertEquals(5, result)
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
		assertEquals(5, result)
	}

	@Test
	fun `compiles assignments to optional member accesses with value`() {
		val sourceCode = """
			SimplestApp object {
				var a = 11
				to getFive(): Int {
					var app: SimplestApp? = SimplestApp
					app?.a = 5
					return a
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `compiles assignments to optional member accesses without value`() {
		val sourceCode = """
			SimplestApp object {
				var a = 11
				to getEleven(): Int {
					var app: SimplestApp? = null
					app?.a = 5
					return a
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEleven")
		assertEquals(11, result)
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
		assertEquals(5, result)
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
		assertEquals(5, result)
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
		assertEquals(5, result)
	}

	@Test
	fun `compiles with primitive generic properties on left hand side`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			SimplestApp object {
				to getEight(): Int {
					val container = Container(12)
					container.a = 8
					return container.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEight")
		assertEquals(8, result)
	}

	@Test
	fun `compiles with primitive generic properties on right hand side`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			SimplestApp object {
				to getSixtyOne(): Int {
					var b = 0
					val container = Container(61)
					b = container.a
					return b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSixtyOne")
		assertEquals(61, result)
	}

	@Test
	fun `compiles with primitive generic index access on left hand side`() {
		val sourceCode = """
			Container class {
				containing Item
				var b: Item
				init(b)
				operator[c: Int](a: Item) {
					b = a
				}
			}
			SimplestApp object {
				to getEightySix(): Int {
					val container = Container(77)
					container[0] = 86
					return container.b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightySix")
		assertEquals(86, result)
	}

	@Test
	fun `converts values`() {
		val sourceCode = """
			A class {
				val b: Int
				converting init(b)
			}
			SimplestApp object {
				to getThirtyFive(): Int {
					var a: A
					a = 35
					return a.b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThirtyFive")
		assertEquals(35, result)
	}
}
