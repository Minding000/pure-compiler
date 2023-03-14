package components.semantic_analysis.resolution

import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.operations.MemberAccess
import logger.Severity
import logger.issues.access.InstanceAccessFromStaticContext
import logger.issues.access.StaticAccessFromInstanceContext
import logger.issues.resolution.NotFound
import logger.issues.resolution.SignatureAmbiguity
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
		lintResult.assertIssueDetected<NotFound>("Initializer 'Item(Item)' hasn't been declared yet.", Severity.ERROR)
	}

	@Test
	fun `resolves unbound initializer calls on unbound type definitions`() {
		val sourceCode =
			"""
				Window class {
					Pane class
				}
				Window.Pane()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<InstanceAccessFromStaticContext>()
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
		lintResult.assertIssueDetected<InstanceAccessFromStaticContext>(
			"Cannot access instance member 'Pane' from static context.", Severity.ERROR)
		lintResult.assertIssueNotDetected<NotFound>()
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
		lintResult.assertIssueNotDetected<StaticAccessFromInstanceContext>()
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
		lintResult.assertIssueDetected<StaticAccessFromInstanceContext>("Accessing static member 'Pane' from instance context.",
			Severity.WARNING)
	}

	@Disabled
	@Test
	fun `resolves initializer calls with a variable number of parameters`() {
		val sourceCode =
			"""
				Int class
				IntegerList class {
					init(...integers: ...Int)
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

					init(index: Int)
					init(element: Element)
				}
				val numbers = <Int>List(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<SignatureAmbiguity>("""
			Call to initializer '<Int>List(Int)' is ambiguous. Matching signatures:
			 - '<Element>List(Int)' declared at Test.Test:5:1
			 - '<Element>List(Int)' declared at Test.Test:6:1
		""".trimIndent(), Severity.ERROR)
	}
}
