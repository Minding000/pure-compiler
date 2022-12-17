package components.semantic_analysis.resolution

import components.semantic_analysis.semantic_model.control_flow.FunctionCall
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
				Item class {
					init() {}
				}
				Item(Item())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Initializer 'Item(Item)' hasn't been declared yet")
	}

	@Test
	fun `resolves initializer calls`() {
		val sourceCode =
			"""
				Int class {
					init
				}
				Window class {
					init(width: Int, height: Int) {}
				}
				Window(Int(), Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall>()
		assertNotNull(initializerCall?.type)
	}

	@Disabled
	@Test
	fun `resolves initializer calls with a variable number of parameters`() {
		val sourceCode =
			"""
				Int class {
					init
				}
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
				Int class {
					init
				}
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
