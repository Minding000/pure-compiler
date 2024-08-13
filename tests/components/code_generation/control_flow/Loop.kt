package components.code_generation.control_flow

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
					loop
						a = 2
				}
			}
		""".trimIndent()
		val intermediateRepresentation = TestUtil.getIntermediateRepresentation(sourceCode)
		assertContains(intermediateRepresentation, """
			define void @"run()"(ptr %0, ptr %1) {
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
		assertEquals(2, result)
	}

	@Test
	fun `break statement jumps to always block within loop`() {
		val sourceCode = """
			SimplestApp object {
				to getOne(): Int {
					return sum(1, 2, 3)
				}
				to sum(...numbers: ...Int): Int {
					var sum = 0
					loop over numbers as number {
						{
							break
						} always {
							sum += number
						}
					}
					return sum
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, result)
	}

	@Test
	fun `break statement doesn't jump to always block outside of loop`() {
		val sourceCode = """
			SimplestApp object {
				to getThirteen(): Int {
					return sum(1, 2, 1)
				}
				to sum(...numbers: ...Int): Int {
					var sum = 0
					{
						loop over numbers as number {
							sum += number
							break
						}
						sum += 10
					} always {
						sum += 2
					}
					return sum
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThirteen")
		assertEquals(13, result)
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
		assertEquals(0, result)
	}

	@Test
	fun `next statement advances loop over plural type`() {
		val sourceCode = """
			SimplestApp object {
				to getSix(): Int {
					return sum(1, 2, 3)
				}
				to sum(...numbers: ...Int): Int {
					var sum = 0
					loop over numbers as number {
						sum += number
						next
					}
					return sum
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, result)
	}

	@Test
	fun `next statement advances loop over iterable`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getSix(): Int {
					return sum(List(1, 2, 3))
				}
				to sum(numbers: <Int>List): Int {
					var sum = 0
					loop over numbers as number {
						sum += number
						next
					}
					return sum
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix", true)
		assertEquals(6, result)
	}

	@Test
	fun `next statement jumps to always block within loop`() {
		val sourceCode = """
			SimplestApp object {
				to getSix(): Int {
					return sum(1, 2, 3)
				}
				to sum(...numbers: ...Int): Int {
					var sum = 0
					loop over numbers as number {
						{
							next
						} always {
							sum += number
						}
					}
					return sum
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, result)
	}

	@Test
	fun `next statement doesn't jump to always block outside of loop`() {
		val sourceCode = """
			SimplestApp object {
				to getSix(): Int {
					return sum(1, 2, 1)
				}
				to sum(...numbers: ...Int): Int {
					var sum = 0
					{
						loop over numbers as number {
							sum += number
							next
						}
					} always {
						sum += 2
					}
					return sum
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, result)
	}

	@Test
	fun `compiles while loops`() {
		val sourceCode = """
			SimplestApp object {
				to run() {
					var x = yes
					loop while x
						x = no
				}
			}
			""".trimIndent()
		val intermediateRepresentation = TestUtil.getIntermediateRepresentation(sourceCode)
		assertContains(intermediateRepresentation, """
			define void @"run()"(ptr %0, ptr %1) {
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
					loop over numbers as number
						sum += number
					return sum
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, result)
	}

	@Test
	fun `provides index when iterating over plural type`() {
		val sourceCode = """
			SimplestApp object {
				to getThree(): Int {
					return countParameters(0, 0, 0)
				}
				to countParameters(...numbers: ...Int): Int {
					var parameterCount = 0
					loop over numbers as index, number
						parameterCount = index + 1
					return parameterCount
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThree")
		assertEquals(3, result)
	}

	@Test
	fun `compiles over loops iterating over collection`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getSix(): Int {
					return sum(List(1, 2, 3))
				}
				to sum(numbers: <Int>List): Int {
					var sum = 0
					loop over numbers as number
						sum += number
					return sum
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix", true)
		assertEquals(6, result)
	}
}
