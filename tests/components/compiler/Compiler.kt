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
		val program = LlvmProgram("Test")
		val constructor = program.constructor
		val argumentTypes = LlvmList<LlvmType>(0)
		val functionType = constructor.buildFunctionType(argumentTypes, 0, constructor.i32Type)
		val function = constructor.buildFunction("getNumber", functionType)
		constructor.createAndSelectBlock(function, "body")
		val number = constructor.buildInt32(expectedResult)
		constructor.buildReturn(number)
		program.entrypoint = function
		program.verify()
		program.compile()
		val result = program.run()
		val intResult = Llvm.castToInt(result)
		program.dispose()
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
		val program = LlvmProgram("Test")
		program.loadSemanticModel(lintResult.program, "Test:SimplestApp.getFive")
		program.verify()
		program.compile()
		val result = program.run()
		val intResult = Llvm.castToInt(result)
		program.dispose()
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
		val program = LlvmProgram("Test")
		program.loadSemanticModel(lintResult.program, "Test:SimplestApp.getFiveOrTen")
		program.verify()
		program.compile()
		val result = program.run()
		val intResult = Llvm.castToInt(result)
		program.dispose()
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
		val program = LlvmProgram("Test")
		program.loadSemanticModel(lintResult.program, "Test:SimplestApp.getTenOrTwelve")
		program.verify()
		program.compile()
		val result = program.run()
		val intResult = Llvm.castToInt(result)
		program.dispose()
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
		val program = LlvmProgram("Test")
		assertDoesNotThrow {
			program.loadSemanticModel(lintResult.program, "Test:SimplestApp.run")
			program.verify()
			program.compile()
			program.run()
			program.dispose()
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
		val lintResult = TestUtil.lint(sourceCode)
		val program = LlvmProgram("Test")
		assertDoesNotThrow {
			program.loadSemanticModel(lintResult.program, "Test:SimplestApp.getFive")
			program.verify()
			program.compile()
			val result = program.run()
			val intResult = Llvm.castToInt(result)
			program.dispose()
			assertEquals(5, intResult)
		}
	}

	@Test
	fun `compiles assignments`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					val five: Int
					five = 5
					return five
				}
			}
		""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val program = LlvmProgram("Test")
		assertDoesNotThrow {
			program.loadSemanticModel(lintResult.program, "Test:SimplestApp.getFive")
			program.verify()
			program.compile()
			val result = program.run()
			val intResult = Llvm.castToInt(result)
			program.dispose()
			assertEquals(5, intResult)
		}
	}
}
