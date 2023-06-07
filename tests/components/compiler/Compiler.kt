package components.compiler

import components.compiler.targets.llvm.Llvm
import components.compiler.targets.llvm.LlvmProgram
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
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFiveOrTen")
		assertEquals(10, Llvm.castToSignedInteger(result))
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
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTenOrTwelve")
		assertEquals(12, Llvm.castToSignedInteger(result))
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
					return pipe(5)
				}
				to pipe(integer: Int): Int {
					return integer
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles infinite loops`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					loop {
						if no
							next
						break
					}
				}
			}
		""".trimIndent()
		assertDoesNotThrow {
			TestUtil.run(sourceCode, "Test:SimplestApp.run")
		}
	}

	@Test
	fun `compiles while loops`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					var x = yes
					loop while x {
						x = no
					}
				}
			}
			""".trimIndent()
		val intermediateRepresentation = """
			%SimplestApp = type {}

			define void @"run()"() {
			function_entry:
			  %x = alloca i1, align 1
			  store i1 true, i1* %x, align 1
			  br label %loop_entry

			loop_entry:                                       ; preds = %loop_body, %function_entry
			  %x1 = load i1, i1* %x, align 1
			  br i1 %x1, label %loop_body, label %loop_exit

			loop_body:                                        ; preds = %loop_entry
			  store i1 false, i1* %x, align 1
			  br label %loop_entry

			loop_exit:                                        ; preds = %loop_entry
			  ret void
			}

			define void @Test() {
			file:
			  %SimplestApp = alloca %SimplestApp, align 8
			  ret void
			}
			""".trimIndent()
		TestUtil.assertIntermediateRepresentationEquals(sourceCode, intermediateRepresentation)
	}
}
