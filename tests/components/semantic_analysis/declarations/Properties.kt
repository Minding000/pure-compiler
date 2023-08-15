package components.semantic_analysis.declarations

import logger.Severity
import logger.issues.modifiers.*
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Properties {

	@Test
	fun `allows for properties to be overridden`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					val nutritionScore: Number
				}
				Vegetable class: Food
				Potato class: Vegetable {
					overriding val nutritionScore: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingOverridingKeyword>()
		lintResult.assertIssueNotDetected<OverriddenSuperMissing>()
		lintResult.assertIssueNotDetected<OverridingPropertyTypeNotAssignable>()
		lintResult.assertIssueNotDetected<OverridingPropertyTypeMismatch>()
		lintResult.assertIssueNotDetected<VariablePropertyOverriddenByValue>()
	}

	@Test
	fun `detects missing overriding keyword on properties`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					val nutritionScore: Number
				}
				Vegetable class: Food
				Potato class: Vegetable {
					val nutritionScore: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingOverridingKeyword>(
			"Property 'nutritionScore: Float' is missing the 'overriding' keyword.")
	}

	@Test
	fun `detects overriding keyword being used without super property`() {
		val sourceCode =
			"""
				Room class {
					overriding val isClean = yes
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverriddenSuperMissing>(
			"'overriding' keyword is used, but the property doesn't have a super property.")
	}

	@Test
	fun `detects overriding value property with incompatible type`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					val nutritionScore: Number
				}
				Potato class: Food {
					overriding val nutritionScore: Food
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingPropertyTypeNotAssignable>(
			"Type of overriding property 'Food' is not assignable to the type of the overridden property 'Number'.",
			Severity.ERROR)
	}

	@Test
	fun `detects overriding variable property with incompatible type`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					var nutritionScore: Number
				}
				Potato class: Food {
					overriding var nutritionScore: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingPropertyTypeMismatch>(
			"Type of overriding property 'Float' does not match the type of the overridden property 'Number'.", Severity.ERROR)
	}

	@Test
	fun `detects variable property being overridden by value property`() {
		val sourceCode =
			"""
				Number class
				Food class {
					var nutritionScore: Number
				}
				Potato class: Food {
					overriding val nutritionScore: Number
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<VariablePropertyOverriddenByValue>(
			"Variable super property 'nutritionScore' cannot be overridden by value property.", Severity.ERROR)
	}
}
