package components.code_generation.operations

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
		assertEquals(12, result)
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
		assertEquals(2, result)
	}

	@Test
	fun `compiles with primitive generic value as provided getter key`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				var b = 0
				init(a)
				operator[c: Int]: Int {
					b = c
					return 0
				}
			}
			SimplestApp object {
				to getEightySix(): Int {
					val container = Container(86)
					container[container.a]
					return container.b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightySix")
		assertEquals(86, result)
	}

	@Test
	fun `compiles with primitive generic value as expected getter key`() {
		val sourceCode = """
			Container class {
				containing Item
				var b: Item
				init(b)
				operator[a: Item]: Int {
					b = a
					return 0
				}
			}
			SimplestApp object {
				to getEightySix(): Int {
					val container = Container(77)
					container[86]
					return container.b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightySix")
		assertEquals(86, result)
	}

	@Test
	fun `compiles with primitive generic value as provided setter key`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				var b = 0
				init(a)
				operator[c: Int](d: Int) {
					b = c
				}
			}
			SimplestApp object {
				to getEightySix(): Int {
					val container = Container(86)
					container[container.a] = 9
					return container.b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightySix")
		assertEquals(86, result)
	}

	@Test
	fun `compiles with primitive generic value as expected setter key`() {
		val sourceCode = """
			Container class {
				containing Item
				var b: Item
				init(b)
				operator[a: Item](c: Int) {
					b = a
				}
			}
			SimplestApp object {
				to getEightySix(): Int {
					val container = Container(77)
					container[86] = 6
					return container.b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightySix")
		assertEquals(86, result)
	}
}
