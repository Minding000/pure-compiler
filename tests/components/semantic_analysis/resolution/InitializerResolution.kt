package components.semantic_analysis.resolution

import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.operations.MemberAccess
import messages.Message
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull

internal class InitializerResolution {

	@Test
	fun `emits error for undeclared initializers`() {
		val sourceCode =
			"""
				Item class
				Item(Item())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Initializer 'Item(Item)' hasn't been declared yet")
	}

	@Test
	fun `resolves unbound initializer calls on unbound type definitions`() { //TODO unbound type properties should be static
		val sourceCode =
			"""
				Window class {
					Pane class
				}
				Window.Pane()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Bound types can only be initialized within their parents instance context")
		val initializerCall = lintResult.find<FunctionCall>()
		assertNotNull(initializerCall?.type)
	}

	@Test
	fun `disallows unbound initializer calls on bound type definitions`() {
		val sourceCode =
			"""
				Window class {
					bound Pane class
				}
				Window.Pane()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Bound types can only be initialized within their parents instance context")
	}

	@Test
	fun `resolves bound initializer calls on bound type definitions`() {
		val sourceCode =
			"""
				Window class {
					bound Pane class
				}
				Window().Pane()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Unbound types can only be initialized within their parents static context")
		val initializerCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertNotNull(initializerCall?.type)
	}

	@Test
	fun `disallows bound initializer calls on unbound type definitions`() {
		val sourceCode =
			"""
				Window class {
					Pane class
				}
				Window().Pane()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Unbound types can only be initialized within their parents static context")
	}

	@Disabled
	@Test
	fun `resolves initializer calls with a variable number of parameters`() {
		val sourceCode =
			"""
				Int class
				IntegerList class {
					init(...integers: ...Int) {}
				}
				IntegerList()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall>()
		assertNotNull(initializerCall?.type)
	}

	@Test
	fun `emits error for ambiguous initializer calls`() {
		val sourceCode =
			"""
				Int class
				List class {
					containing Element

					init(index: Int) {}
					init(element: Element) {}
				}
				val numbers = <Int>List(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Call to initializer '<Int>List(Int)' is ambiguous")
	}
}
