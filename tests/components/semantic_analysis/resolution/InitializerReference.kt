package components.semantic_analysis.resolution

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class InitializerReference {

	@Test
	fun `allows initializer reference inside of initializers`() {
		val sourceCode =
			"""
				Car class {
					var name = ""
					init
					init(name) {
						init()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Initializer references are not allowed outside of initializers")
	}

	@Test
	fun `disallows initializer reference outside of initializers`() {
		val sourceCode =
			"""
				Car class {
					to startEngine() {
						init()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Initializer references are not allowed outside of initializers")
	}
}
