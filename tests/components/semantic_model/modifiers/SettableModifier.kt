package components.semantic_model.modifiers

import logger.issues.modifiers.DisallowedModifier
import org.junit.jupiter.api.Test
import util.TestUtil

internal class SettableModifier {

	@Test
	fun `is not allowed on classes`() {
		val sourceCode = "settable Goldfish class"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on objects`() {
		val sourceCode = "settable Earth object"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on enums`() {
		val sourceCode = "settable Tire enum"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on initializers`() {
		val sourceCode =
			"""
				Mask class {
					settable init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on instance lists`() {
		val sourceCode =
			"""
				Number class {
					settable instances ZERO
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
					settable val brain: Brain
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on computed properties`() {
		val sourceCode =
			"""
				Goldfish class {
					settable computed name: String
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on functions`() {
		val sourceCode =
			"""
				Goldfish class {
					settable to swim()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}
}
