package components.semantic_analysis.types

import components.semantic_analysis.semantic_model.definitions.TypeAlias
import messages.Message
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
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
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'Handler' has no type, because it's part of a circular alias")
		val typeAlias = lintResult.find<TypeAlias>()
		assertNotNull(typeAlias?.referenceType)
	}
}
