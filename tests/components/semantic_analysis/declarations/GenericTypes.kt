package components.semantic_analysis.declarations

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class GenericTypes {

	@Test
	fun `allows generic types in classes`() {
		val sourceCode =
			"""
				Human class {
					containing JobType
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING,
			"Generic type declarations are not allowed")
	}

	@Test
	fun `disallows generic types in enums`() {
		val sourceCode =
			"""
				Mood enum {
					containing JobType
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING,
			"Generic type declarations are not allowed")
	}

	@Test
	fun `emits warning for generic types in objects`() {
		val sourceCode =
			"""
				Earth object {
					containing Species
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"Generic type declarations are not allowed in objects")
	}
}
