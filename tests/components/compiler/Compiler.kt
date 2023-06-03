package components.compiler

import components.compiler.targets.llvm.Llvm
import components.compiler.targets.llvm.LlvmList
import components.compiler.targets.llvm.LlvmProgram
import components.compiler.targets.llvm.LlvmType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import util.TestUtil
import kotlin.test.assertEquals

internal class Compiler {

	@Test
	fun `is able to assemble and run test program`() {
		val expectedResult = 5L
		val context = LlvmProgram("Test")
		val constructor = context.constructor
		val argumentTypes = LlvmList<LlvmType>(0)
		val functionType = constructor.buildFunctionType(argumentTypes, 0, constructor.i32Type)
		val function = constructor.buildFunction("getNumber", functionType)
		constructor.createAndSelectBlock(function, "body")
		val number = constructor.buildInt32(expectedResult)
		constructor.buildReturn(number)
		context.entrypoint = function
		context.verify()
		context.compile()
		val result = context.run()
		val intResult = Llvm.castToInt(result)
		context.dispose()
		assertEquals(expectedResult, intResult)
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
		val lintResult = TestUtil.lint(sourceCode)
		val context = LlvmProgram("Test")
		context.loadSemanticModel(lintResult.program, "Test:SimplestApp.getFive")
		context.verify()
		context.compile()
		val result = context.run()
		val intResult = Llvm.castToInt(result)
		context.dispose()
		assertEquals(5, intResult)
	}

	@Test
	fun `compiles if statements without negative branch`() {
		val sourceCode = """
			SimplestApp object {
				to getFiveOrTen(): Int {
					if yes
						return 10
					return 5
				}
			}
		""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val context = LlvmProgram("Test")
		context.loadSemanticModel(lintResult.program, "Test:SimplestApp.getFiveOrTen")
		context.verify()
		context.compile()
		val result = context.run()
		val intResult = Llvm.castToInt(result)
		context.dispose()
		assertEquals(10, intResult)
	}

	@Test
	fun `compiles if statements with negative branch`() {
		val sourceCode = """
			SimplestApp object {
				to getTenOrTwelve(): Int {
					if no
						return 10
					else
						return 12
				}
			}
		""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val context = LlvmProgram("Test")
		context.loadSemanticModel(lintResult.program, "Test:SimplestApp.getTenOrTwelve")
		context.verify()
		context.compile()
		val result = context.run()
		val intResult = Llvm.castToInt(result)
		context.dispose()
		assertEquals(12, intResult)
	}

	@Test
	fun `compiles function with implicit return`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
				}
			}
		""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val context = LlvmProgram("Test")
		assertDoesNotThrow {
			context.loadSemanticModel(lintResult.program, "Test:SimplestApp.run")
			context.verify()
			context.compile()
			context.run()
			context.dispose()
		}
	}

	@Test
	fun `compiles function with variable`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					val five = 5
					return five
				}
			}
		""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val context = LlvmProgram("Test")
		assertDoesNotThrow {
			context.loadSemanticModel(lintResult.program, "Test:SimplestApp.getFive")
			context.verify()
			context.compile()
			val result = context.run()
			val intResult = Llvm.castToInt(result)
			context.dispose()
			assertEquals(5, intResult)
		}
	}
}
