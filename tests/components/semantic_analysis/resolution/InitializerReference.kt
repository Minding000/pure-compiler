package components.semantic_analysis.resolution

import logger.Severity
import logger.issues.resolution.InitializerReferenceOutsideOfInitializer
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
		lintResult.assertIssueNotDetected<InitializerReferenceOutsideOfInitializer>()
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
		lintResult.assertIssueDetected<InitializerReferenceOutsideOfInitializer>(
			"Initializer references are not allowed outside of initializers.", Severity.ERROR)
	}
}
