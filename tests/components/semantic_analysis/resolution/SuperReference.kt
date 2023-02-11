package components.semantic_analysis.resolution

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class SuperReference {

	@Test
	fun `allows super initializer reference inside of initializer`() {
		val sourceCode =
			"""
				Vehicle class
				Car class: Vehicle {
					init {
						super.init()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Super references are not allowed outside of type definitions")
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Super references are not allowed outside of member accesses")
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "super initializer can only be called from initializers")
	}

	@Test
	fun `disallows super reference keyword outside of type definition`() {
		val sourceCode =
			"""
				super
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Super references are not allowed outside of type definitions")
	}

	@Test
	fun `disallows super reference keyword outside of member accesses`() {
		val sourceCode =
			"""
				Car class {
					to getCar(): Car {
						return super
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Super references are not allowed outside of member accesses")
	}

	@Test
	fun `disallows super initializer reference outside of initializer`() {
		val sourceCode =
			"""
				Car class {
					to getCar(): Car {
						super.init()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "The super initializer can only be called from initializers")
	}
}
