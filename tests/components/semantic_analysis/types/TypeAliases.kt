package components.semantic_analysis.types

import logger.Severity
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.definition.CircularTypeAlias
import logger.issues.resolution.NotCallable
import org.junit.jupiter.api.Test
import util.TestUtil

internal class TypeAliases {

	@Test
	fun `complex types can be assigned to type aliases`() {
		val sourceCode =
			"""
				Event class
				alias EventHandler = (Event) =>|
				val complexTypeValue: (Event) =>|
				var typeAliasValue: EventHandler = complexTypeValue
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `type aliases can be assigned to complex types`() {
		val sourceCode =
			"""
				Event class
				alias EventHandler = (Event) =>|
				val typeAliasValue: EventHandler
				var complexTypeValue: (Event) =>| = typeAliasValue
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `allows aliasing type aliases`() {
		val sourceCode =
			"""
				val typeAliasValue: EventProcessor
				var complexTypeValue: (Event) =>| = typeAliasValue
				alias EventProcessor = EventHandler
				alias EventHandler = (Event) =>|
				Event class
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `resolves calls to values with aliased function type`() {
		val sourceCode =
			"""
				alias ClickHandler = =>|
				val handler: ClickHandler
				handler()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotCallable>()
	}

	@Test
	fun `allows recursive type aliases`() {
		val sourceCode =
			"""
				Leaf class
				List class {
					containing Element
				}
				alias TreeNode = Leaf | List<TreeNode>
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<CircularTypeAlias>()
	}

	@Test
	fun `disallows circular type aliases`() {
		val sourceCode =
			"""
				alias Handler = Processor
				alias Processor = Handler
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<CircularTypeAlias>("'Handler' has no type, because it's part of a circular assignment.",
			Severity.ERROR)
		lintResult.assertIssueDetected<CircularTypeAlias>(
			"'Processor' has no type, because it's part of a circular assignment.", Severity.ERROR)
	}
}
