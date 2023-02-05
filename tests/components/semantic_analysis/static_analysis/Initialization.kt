package components.semantic_analysis.static_analysis

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Initialization {

	@Test
	fun `allows use of initialized local variable`() {
		val sourceCode =
			"""
				val x = 5
				x
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "hasn't been initialized")
	}

	@Test
	fun `disallows use of uninitialized local variable`() {
		val sourceCode =
			"""
				Int class
				val x: Int
				x
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Local variable 'x' hasn't been initialized yet")
	}

	@Test
	fun `allows assignment of uninitialized constant local variable`() {
		val sourceCode =
			"""
				Int class
				val x: Int
				x = 4
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "cannot be reassigned, because it is constant")
	}

	@Test
	fun `disallows assignment of possibly initialized constant local variable`() {
		val sourceCode =
			"""
				Int class
				val x: Int
				if yes
					x = 2
				x = 4
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "cannot be reassigned, because it is constant")
	}

	@Test
	fun `allows assignment of initialized local variable`() {
		val sourceCode =
			"""
				Int class
				var x = 2
				x = 4
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "cannot be reassigned, because it is constant")
	}

	@Test
	fun `disallows assignment of initialized constant local variable`() {
		val sourceCode =
			"""
				val x = 2
				x = 4
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'x' cannot be reassigned, because it is constant.")
	}

	@Test
	fun `disallows initialization of constant property outside of initializer`() {
		val sourceCode =
			"""
				Int class
				Human class {
					val numberOfArms: Int

					to talk() {
						numberOfArms = 3
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'numberOfArms' cannot be reassigned, because it is constant")
	}

	@Test
	fun `allows initialization of uninitialized constant property inside of initializer`() {
		val sourceCode =
			"""
				Int class
				Human class {
					val numberOfArms: Int

					init {
						numberOfArms = 3
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "cannot be reassigned, because it is constant")
	}

	@Test
	fun `disallows initialization of initialized constant property inside of initializer`() {
		val sourceCode =
			"""
				Int class
				Human class {
					val numberOfArms = 2

					init {
						numberOfArms = 3
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'numberOfArms' cannot be reassigned, because it is constant")
	}

	//TODO check if all properties get initialized in the initializer
	//TODO check if functions use uninitialized properties in the initializer
	//TODO check that function parameters are treated as local constants
}
