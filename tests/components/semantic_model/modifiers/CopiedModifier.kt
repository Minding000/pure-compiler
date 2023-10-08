package components.semantic_model.modifiers

import logger.issues.modifiers.DisallowedModifier
import org.junit.jupiter.api.Test
import util.TestUtil

internal class CopiedModifier {

	@Test
	fun `is allowed on classes`() {
		val sourceCode = "copied Goldfish class"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on objects`() {
		val sourceCode = "copied Earth object"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on enums`() {
		val sourceCode = "copied Tire enum"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on initializers`() {
		val sourceCode =
			"""
				Mask class {
					copied init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on properties`() {
		val sourceCode =
			"""
				Goldfish class {
					copied val brain: Brain
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on computed properties`() {
		val sourceCode =
			"""
				Goldfish class {
					copied computed name: String
						gets "Bernd"
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on functions`() {
		val sourceCode =
			"""
				Goldfish class {
					copied to swim()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}
}
