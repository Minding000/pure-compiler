package components.semantic_model.types

import logger.Severity
import logger.issues.declaration.InvalidSelfTypeLocation
import org.junit.jupiter.api.Test
import util.TestUtil

internal class SelfTypes {

	@Test
	fun `self types can be used in classes`() {
		val sourceCode =
			"""
				Camera class {
					to getDevice(): Self
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<InvalidSelfTypeLocation>()
	}

	@Test
	fun `self types can not be used outside of classes`() {
		val sourceCode =
			"""
				val camera: Self
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<InvalidSelfTypeLocation>("The self type is only allowed in type declarations.",
			Severity.ERROR)
	}
}
