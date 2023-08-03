package components.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import util.TestUtil
import kotlin.test.assertContains

internal class Loop {

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
}
