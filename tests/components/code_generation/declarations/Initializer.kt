package components.code_generation.declarations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Initializer {

	@Test
	fun `initializes property with value of property parameter`() {
		val sourceCode = """
			Application class {
				val id: Int
				init(id)
			}
			SimplestApp object {
				to getA(): Int {
					val application = Application(53)
					return application.id
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(53, result)
	}

	@Test
	fun `compiles explicit super initializer calls`() {
		val sourceCode = """
			Application class {
				val a: Int
				init {
					a = 74
				}
			}
			SimplestApp object: Application {
				init {
					super.init()
				}
				to getA(): Int {
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(74, result)
	}

	@Test
	fun `default initializer calls super initializer`() {
		val sourceCode = """
			Application class {
				val a: Int
				init {
					a = 74
				}
			}
			SimplestApp object: Application {
				to getA(): Int {
					return a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(74, result)
	}

	@Test
	fun `supports variadic parameters`() {
		val sourceCode = """
			Summer class {
				var sum = 0
				init(...numbers: ...Int) {
					loop over numbers as number {
						sum += number
					}
				}
			}
			SimplestApp object {
				to getSum(): Int {
					val summer = Summer(1, 2, 4, 8)
					return summer.sum
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSum")
		assertEquals(15, result)
	}

	@Test
	fun `primitive initializer calls return primitive value`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getTwo(): Int {
					return 1 + Int(1)
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTwo", true)
		assertEquals(2, result)
	}

	@Test
	fun `compiles primitive type alias property parameters`() {
		val sourceCode = """
			native copied Int class {
				native init(value: Int)
			}
			alias ExitCode = Int {
				instances UNKNOWN_ARGUMENT(8)
			}
			Program class {
				val exitCode: ExitCode
				init(exitCode)
			}
			SimplestApp object {
				to getEight(): Int {
					return Program(.UNKNOWN_ARGUMENT).exitCode
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEight", mapOf(
			SpecialType.INTEGER to TestUtil.TEST_FILE_NAME
		))
		assertEquals(8, result)
	}
}
