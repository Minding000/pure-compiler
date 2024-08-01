package components.semantic_model.modifiers

import logger.issues.modifiers.DisallowedModifier
import org.junit.jupiter.api.Test
import util.TestUtil

internal class NativeModifier {

	@Test
	fun `is allowed on classes`() {
		val sourceCode = "native Goldfish class"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on objects`() {
		val sourceCode = "native Earth object"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on enums`() {
		val sourceCode = "native Tire enum"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on properties`() {
		val sourceCode =
			"""
				Goldfish class {
					native val brain: Brain
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
					native computed name: String
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on instance lists`() {
		val sourceCode =
			"""
				Int class {
					native instances ZERO
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
					native init()
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
					native to swim()
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
					native operator ++
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}
}
