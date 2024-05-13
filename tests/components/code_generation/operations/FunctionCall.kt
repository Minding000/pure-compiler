package components.code_generation.operations

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class FunctionCall {

	@Test
	fun `compiles function call with primitive generic value as provided parameter`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				var b = 0
				init(a)
				to set(c: Int) {
					b = c
				}
			}
			SimplestApp object {
				to getEightySix(): Int {
					val container = Container(86)
					container.set(container.a)
					return container.b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightySix")
		assertEquals(86, result)
	}

	@Test
	fun `compiles function call with primitive generic value as expected parameter`() {
		val sourceCode = """
			Container class {
				containing Item
				var b: Item
				init(b)
				to set(a: Item) {
					b = a
				}
			}
			SimplestApp object {
				to getEightySix(): Int {
					val container = Container(77)
					container.set(86)
					return container.b
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightySix")
		assertEquals(86, result)
	}

	@Test
	fun `compiles initializer call with primitive generic value as provided parameter`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			Proxy class {
				var a: Int
				init(a)
			}
			SimplestApp object {
				to getEightySix(): Int {
					val container = Container(86)
					val proxy = Proxy(container.a)
					return proxy.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightySix")
		assertEquals(86, result)
	}

	@Test
	fun `compiles initializer call with primitive generic value as expected parameter`() {
		val sourceCode = """
			Container class {
				containing Item
				var a: Item
				init(a)
			}
			SimplestApp object {
				to getEightySix(): Int {
					val container = Container(86)
					return container.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightySix")
		assertEquals(86, result)
	}
}
