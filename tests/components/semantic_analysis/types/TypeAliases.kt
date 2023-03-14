package components.semantic_analysis.types

import components.semantic_analysis.semantic_model.definitions.TypeAlias
import logger.issues.constant_conditions.TypeNotAssignable
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull

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

	@Disabled
	@Test
	fun `disallows circular type aliases`() {
		// Should this really be disallowed?
		// e.g. alias TreeNode = Leaf | List<TreeNode>
		val sourceCode =
			"""
				alias Handler = Processor
				alias Processor = Handler
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		//lintResult.assertIssueDetected<>("'Handler' has no type, because it's part of a circular alias.", Severity.ERROR)
		val typeAlias = lintResult.find<TypeAlias>()
		assertNotNull(typeAlias?.referenceType)
	}
}
