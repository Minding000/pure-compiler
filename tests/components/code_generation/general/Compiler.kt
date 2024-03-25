package components.code_generation.general

import components.code_generation.llvm.Llvm
import components.code_generation.llvm.LlvmProgram
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import util.TestUtil
import kotlin.test.assertEquals

internal class Compiler {

	@Test
	fun `is able to assemble and run test program`() {
		val expectedResult = 5L
		val program = LlvmProgram(TestUtil.TEST_PROJECT_NAME)
		val constructor = program.constructor
		val functionType = constructor.buildFunctionType(emptyList(), constructor.i32Type)
		val function = constructor.buildFunction("getNumber", functionType)
		constructor.createAndSelectBlock(function, "body")
		val number = constructor.buildInt32(expectedResult)
		constructor.buildReturn(number)
		program.entrypoint = function
		program.verify()
		program.compile()
		val result = program.run()
		program.dispose()
		assertEquals(expectedResult, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles functions`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					return 5
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles function with implicit return`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
				}
			}
		""".trimIndent()
		assertDoesNotThrow {
			TestUtil.run(sourceCode, "Test:SimplestApp.run")
		}
	}

	@Test
	fun `compiles variables`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					val five = 5
					return five
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles function calls`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					return createFive()
				}
				to createFive(): Int {
					return 5
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles function calls with parameters`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					return pipe(5, 2)
				}
				to pipe(integer: Int, unused: Int): Int {
					return integer
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}
}
