package components.semantic_model.types

import components.semantic_model.declarations.Instance
import components.semantic_model.operations.InstanceAccess
import components.semantic_model.operations.MemberAccess
import logger.Severity
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.declaration.CircularTypeAlias
import logger.issues.resolution.NotCallable
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
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

	@Test
	fun `allows instances to access constructor of aliased type`() {
		val sourceCode =
			"""
				referencing Pure
				alias ExitCode = Int {
					instances SUCCESS(0), ERROR(1)
				}
				ExitCode.SUCCESS
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val instance = lintResult.find<Instance>()
		assertNotNull(instance)
		assertNotNull(instance.initializer)
	}

	@Test
	fun `allows instances to be accessed directly`() {
		val sourceCode =
			"""
				Int class
				alias ExitCode = Int {
					instances SUCCESS(), ERROR()
				}
				ExitCode.SUCCESS
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val memberAccess = lintResult.find<MemberAccess>()
		assertNotNull(memberAccess)
		assertEquals("ExitCode", memberAccess.providedType.toString())
	}

	@Test
	fun `allows instances to be inferred`() {
		val sourceCode =
			"""
				alias ExitCode = Int {
					instances SUCCESS(0), ERROR(1)
				}
				Process object {
					to exit(exitCode: ExitCode)
				}
				Process.exit(.SUCCESS)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val instanceAccess = lintResult.find<InstanceAccess>()
		assertNotNull(instanceAccess)
		assertEquals("ExitCode", instanceAccess.providedType.toString())
	}
}
