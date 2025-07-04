package components.code_generation.operations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class FunctionDefinitionCall {

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

	@Test
	fun `converts values`() {
		val sourceCode = """
			A class {
				val b: Int
				converting init(b)
				it isEqualTo(other: A): Bool {
					return b == other.b
				}
			}
			SimplestApp object {
				to getYes(): Bool {
					return A(5).isEqualTo(5)
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, result)
	}

	@Test
	fun `compiles function calls on primitives`() {
		val sourceCode = """
			native copied Int class {
				native init(value: Int)
				it doubled(): Int {
					return this * 2
				}
			}
			SimplestApp object {
				to getFour(): Int {
					return 2.doubled()
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFour", listOf(SpecialType.INTEGER))
		assertEquals(4, result)
	}
}
