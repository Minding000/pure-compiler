package components.code_generation.control_flow

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class SwitchExpression {

	@Test
	fun `compiles exhaustive switch statements without else branch`() {
		val sourceCode = """
			SimplestApp object {
				to getFiveOrTen(): Int {
					switch yes {
						yes: return 10
						no: return 5
					}
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFiveOrTen")
		assertEquals(10, result)
	}

	@Test
	fun `compiles non-exhaustive switch statements without else branch`() {
		val sourceCode = """
			SimplestApp object {
				to getFiveOrTen(): Int {
					switch yes {
						yes: return 10
					}
					return 5
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFiveOrTen")
		assertEquals(10, result)
	}

	@Test
	fun `compiles exhaustive switch statements with else branch`() {
		val sourceCode = """
			SimplestApp object {
				to getTenOrTwelve(): Int {
					switch no {
						yes: return 10
						no: return 12
						else: return 15
					}
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTenOrTwelve")
		assertEquals(12, result)
	}

	@Test
	fun `compiles non-exhaustive switch statements with else branch`() {
		val sourceCode = """
			SimplestApp object {
				to getTenOrFifteen(): Int {
					switch no {
						no: return 10
						else: return 15
					}
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTenOrFifteen")
		assertEquals(10, result)
	}

	@Test
	fun `compiles switch expressions that don't interrupt execution`() {
		val sourceCode = """
			SimplestApp object {
				to getTenOrTwelve(): Int {
					return switch yes {
						no: 10
						yes: 12
					}
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTenOrTwelve")
		assertEquals(12, result)
	}

	@Test
	fun `compiles switch expressions that may interrupt execution`() {
		val sourceCode = """
			SimplestApp object {
				to getTenOrFortyOne(): Int {
					return switch no {
						yes: 10
						no: return 41
					}
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTenOrFortyOne")
		assertEquals(41, result)
	}
}
