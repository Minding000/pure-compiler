package components.semantic_model.modifiers

import logger.Severity
import logger.issues.declaration.MonomorphicInheritance
import logger.issues.modifiers.DisallowedModifier
import logger.issues.modifiers.MissingMonomorphicKeyword
import org.junit.jupiter.api.Test
import util.TestUtil

internal class MonomorphicModifier {

	@Test
	fun `is not allowed on classes`() {
		val sourceCode = "monomorphic Goldfish class"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on objects`() {
		val sourceCode = "monomorphic Earth object"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on enums`() {
		val sourceCode = "monomorphic Tire enum"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on properties`() {
		val sourceCode =
			"""
				Goldfish class {
					monomorphic val brain: Brain
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
					monomorphic computed name: String
						gets "Bernd"
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on initializers`() {
		val sourceCode =
			"""
				Dictionary class {
					monomorphic init()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on functions`() {
		val sourceCode =
			"""
				Goldfish class {
					monomorphic to swim()
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
					monomorphic operator ++
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `doesn't require function without a self type parameter to be marked as monomorphic`() {
		val sourceCode =
			"""
				abstract Number class
				Int class: Number {
					native to add(other: Int): Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingMonomorphicKeyword>()
	}

	@Test
	fun `detects function taking self type parameters without being marked as monomorphic`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract operator +(other: Self): Self
				}
				Int class: Number {
					native to add(other: Int): Int
					overriding operator +(other: Self): Self {
						return add(other)
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingMonomorphicKeyword>(
			"Operator 'Int + Self: Self' is missing the 'monomorphic' keyword.", Severity.ERROR)
	}

	@Test
	fun `allows monomorphic function to take self type parameters`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic operator +(other: Self): Self
				}
				Int class: Number {
					native to add(other: Int): Int
					overriding monomorphic operator +(other: Self): Self {
						return add(other)
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingMonomorphicKeyword>()
	}

	@Test
	fun `disallows inheriting from non-abstract classes with monomorphic members`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract monomorphic operator +(other: Self): Self
				}
				Int class: Number {
					native to add(other: Int): Int
					overriding monomorphic operator +(other: Self): Self {
						return add(other)
					}
				}
				SafeInt class: Int
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MonomorphicInheritance>("Class 'Int' cannot be inherited from.", Severity.ERROR)
	}
}
