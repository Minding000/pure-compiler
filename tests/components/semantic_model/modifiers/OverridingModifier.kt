package components.semantic_model.modifiers

import logger.issues.modifiers.DisallowedModifier
import org.junit.jupiter.api.Test
import util.TestUtil

internal class OverridingModifier {

	@Test
	fun `is not allowed on classes`() {
		val sourceCode = "overriding Goldfish class"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on objects`() {
		val sourceCode = "overriding Earth object"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on enums`() {
		val sourceCode = "overriding Tire enum"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on properties`() {
		val sourceCode =
			"""
				Goldfish class {
					overriding val brain: Brain
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on computed properties`() {
		val sourceCode =
			"""
				Goldfish class {
					overriding computed name: String
						gets "Bernd"
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on initializers`() {
		val sourceCode =
			"""
				Dictionary class {
					overriding init()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on functions`() {
		val sourceCode =
			"""
				Goldfish class {
					overriding to swim()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on operators`() {
		val sourceCode =
			"""
				Goldfish class {
					overriding operator ++
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}
}
