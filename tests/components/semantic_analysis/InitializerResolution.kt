package components.semantic_analysis

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
				class Item {
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
				native class Int {
					init
				}
				class Window {
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
				native class Int {
					init
				}
				class IntegerList {
					init(...integers: ...Int) {}
				}
				IntegerList()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall>()
		assertNotNull(initializerCall?.type)
	}
}
