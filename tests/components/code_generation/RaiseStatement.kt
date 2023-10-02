package components.code_generation

import org.junit.jupiter.api.Test
import util.TestUtil

internal class RaiseStatement {

	@Test
	fun `compiles raise in function without return type`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					raise 1
				}
			}
			""".trimIndent()
		TestUtil.run(sourceCode, "Test:SimplestApp.run")
	}

	@Test
	fun `compiles raise in function with object return type`() {
		val sourceCode = """
			Drawer class
			SimplestApp object {
				to run(): Drawer {
					raise 1
				}
			}
			""".trimIndent()
		TestUtil.run(sourceCode, "Test:SimplestApp.run")
	}

	@Test
	fun `compiles raise in function with boolean return type`() {
		val sourceCode = """
			SimplestApp object {
				to run(): Bool {
					raise 1
				}
			}
			""".trimIndent()
		TestUtil.run(sourceCode, "Test:SimplestApp.run")
	}

	@Test
	fun `compiles raise in function with byte return type`() {
		val sourceCode = """
			SimplestApp object {
				to run(): Byte {
					raise 1
				}
			}
			""".trimIndent()
		TestUtil.run(sourceCode, "Test:SimplestApp.run")
	}

	@Test
	fun `compiles raise in function with integer return type`() {
		val sourceCode = """
			SimplestApp object {
				to run(): Int {
					raise 1
				}
			}
			""".trimIndent()
		TestUtil.run(sourceCode, "Test:SimplestApp.run")
	}

	@Test
	fun `compiles raise in function with float return type`() {
		val sourceCode = """
			SimplestApp object {
				to run(): Float {
					raise 1
				}
			}
			""".trimIndent()
		TestUtil.run(sourceCode, "Test:SimplestApp.run")
	}
}
