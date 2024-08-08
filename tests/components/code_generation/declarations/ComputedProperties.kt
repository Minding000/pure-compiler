package components.code_generation.declarations

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class ComputedProperties {

	@Test
	fun `compiles computed property getter access on variable`() {
		val sourceCode = """
			SimplestApp object {
				val three = 3
				computed six: Int gets three * 2
				to getSix(): Int {
					return six
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, result)
	}

	@Test
	fun `compiles computed property setter access on variable`() {
		val sourceCode = """
			SimplestApp object {
				computed full: Int sets half = full / 2
				val half = 2
				to getSix(): Int {
					full = 12
					return half
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, result)
	}

	@Test
	fun `compiles computed property getter access on member access`() {
		val sourceCode = """
			SimplestApp object {
				val three = 3
				computed six: Int gets three * 2
				to getSix(): Int {
					return this.six
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, result)
	}

	@Test
	fun `compiles computed property getter access on optional member access`() {
		val sourceCode = """
			SimplestApp object {
				val three = 3
				computed six: Int gets three * 2
				to getSix(): Int {
					val app: SimplestApp? = SimplestApp
					return app?.six ?? 0
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, result)
	}

	@Test
	fun `compiles computed property setter access on member access`() {
		val sourceCode = """
			SimplestApp object {
				computed full: Int sets half = this.full / 2
				val half = 2
				to getSix(): Int {
					this.full = 12
					return half
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, result)
	}

	@Test
	fun `unwraps primitive object returned from generic getter`() {
		val sourceCode = """
			Container class {
				containing Element
				var content: Element
				init(content)
			}
			SimplestApp object {
				to getFortyOne(): Int {
					val container = Container(41)
					return container.content
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFortyOne")
		assertEquals(41, result)
	}

	@Test
	fun `unwraps generic values`() {
		val sourceCode = """
			abstract C class {
				containing A
				abstract gettable computed e: A
			}
			D class: <Int>C {
				overriding computed e: Int
					gets { return 93 }
			}
			SimplestApp object {
				to getNinetyThree(): Int {
					val d: <Int>C = D()
					return d.e
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyThree")
		assertEquals(93, result)
	}

	@Test
	fun `unwraps generic values `() {
		val sourceCode = """
			abstract C class {
				containing A
				abstract gettable computed e: A
			}
			D class: <Int>C {
				overriding computed e: Int
					gets { return 93 }
			}
			SimplestApp object {
				to getNinetyThree(): Int {
					val d: <Int>C = D()
					return d.e
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNinetyThree")
		assertEquals(93, result)
	}
}
