package components.code_generation.operations

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class UnaryModifications {

	@Test
	fun `compiles byte increments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Byte {
					var a: Byte = 4
					a++
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `compiles byte decrements`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Byte {
					var a: Byte = 6
					a--
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `compiles integer increments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var a = 4
					a++
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `compiles integer decrements`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var a = 6
					a--
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `compiles primitive unary modifications on member accesses`() {
		val sourceCode = """
			City object {
				var residentCount = 0
			}
			SimplestApp object {
				to getOne(): Int {
					City.residentCount++
					return City.residentCount
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, result)
	}

	@Test
	fun `compiles custom operator calls`() {
		val sourceCode = """
			QuadraticCounter class {
				var linearCounter = 1
				operator ++ {
					linearCounter++
				}
				to getCount(): Int {
					return linearCounter * linearCounter
				}
			}
			SimplestApp object {
				to getFour(): Int {
					val counter = QuadraticCounter()
					counter++
					return counter.getCount()
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFour")
		assertEquals(4, result)
	}

	@Test
	fun `compiles modification on primitive generic properties`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			SimplestApp object {
				to getOne(): Int {
					val container = Container(0)
					container.a++
					return container.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, result)
	}
}
