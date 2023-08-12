package components.compiler

import components.compiler.targets.llvm.Llvm
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertContains
import kotlin.test.assertEquals

internal class Loop {

	@Test
	fun `compiles infinite loops`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					var a = 1
					loop {
						a = 2
					}
				}
			}
		""".trimIndent()
		val intermediateRepresentation = TestUtil.getIntermediateRepresentation(sourceCode)
		assertContains(intermediateRepresentation, """
			define void @"run()"(ptr %0) {
			entrypoint:
			  %a_Variable = alloca i32, align 4
			  store i32 1, ptr %a_Variable, align 4
			  br label %loop_entry

			loop_entry:                                       ; preds = %loop_entry, %entrypoint
			  store i32 2, ptr %a_Variable, align 4
			  br label %loop_entry
			}
			""".trimIndent())
	}

	@Test
	fun `compiles break statements`() {
		val sourceCode = """
			SimplestApp object {
				to getTwo(): Int {
					var a = 1
					loop {
						a = 2
						break
						a = 3
					}
					return a
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTwo")
		assertEquals(2, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `compiles next statements`() {
		val sourceCode = """
			SimplestApp object {
				to getZero(): Int {
					var a = 1
					loop while a != 0 {
						if a == 2 {
							a = 0
							next
						}
						a = 2
					}
					return a
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getZero")
		assertEquals(0, Llvm.castToSignedInteger(result))
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
		val intermediateRepresentation = TestUtil.getIntermediateRepresentation(sourceCode)
		assertContains(intermediateRepresentation, """
			define void @"run()"(ptr %0) {
			entrypoint:
			  %x_Variable = alloca i1, align 1
			  store i1 true, ptr %x_Variable, align 1
			  br label %loop_entry

			loop_entry:                                       ; preds = %loop_body, %entrypoint
			  %x = load i1, ptr %x_Variable, align 1
			  br i1 %x, label %loop_body, label %loop_exit

			loop_body:                                        ; preds = %loop_entry
			  store i1 false, ptr %x_Variable, align 1
			  br label %loop_entry

			loop_exit:                                        ; preds = %loop_entry
			  ret void
			}
			""".trimIndent())
	}

	@Test
	fun `compiles over loops iterating over plural type`() {
		val sourceCode = """
			SimplestApp object {
				to getSix(): Int {
					return sum(1, 2, 3)
				}
				to sum(...numbers: ...Int): Int {
					var sum = 0
					loop over numbers as number {
						sum += number
					}
					return sum
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, Llvm.castToSignedInteger(result))
	}

	@Disabled
	@Test
	fun `compiles over loops iterating over collection`() {
		val sourceCode = """
			SimplestApp object {
				to getSix(): Int {
					return sum(List(1, 2, 3))
				}
				to sum(numbers: <Int>List): Int {
					var sum = 0
					loop over numbers as number {
						sum += number
					}
					return sum
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, Llvm.castToSignedInteger(result))
	}
}
