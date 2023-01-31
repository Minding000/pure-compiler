package components.semantic_analysis.operations

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Assignments {

	@Test
	fun `emits error for incompatible source expression type`() {
		val sourceCode =
			"""
				val a = 5
				var b = "I'm not a number"
				b = a
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Type 'Int' is not assignable to type 'String'")
	}

	@Test
	fun `emits error for assignment to constant target variable`() {
		val sourceCode =
			"""
				val a = 5
				val b = 4
				b = a
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'b' cannot be reassigned, because it is constant")
	}

	@Test
	fun `allows assignment to uninitialized constant target variable in initializer`() {
		val sourceCode =
			"""
				Int class {
					init
				}
				Switch class {
					val portCount: Int
					init {
						portCount = Int()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "cannot be reassigned")
	}

	@Test
	fun `emits error for assignment to constant target variable outside of initializer`() {
		val sourceCode =
			"""
				Int class {
					init
				}
				Switch class {
					val portCount: Int
					init {
						portCount = Int()
					}
					to addPort() {
						portCount = portCount + 1
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'portCount' cannot be reassigned, because it is constant")
	}

	@Test
	fun `emits error for assignment to initialized constant target variable in initializer`() {
		val sourceCode =
			"""
				Int class {
					init
				}
				Switch class {
					val portCount = Int()
					init {
						portCount = Int()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'portCount' cannot be reassigned, because it is constant")
	}
}
