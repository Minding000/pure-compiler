package components.semantic_model.resolution

import components.semantic_model.operations.FunctionCall
import components.semantic_model.operations.MemberAccess
import components.semantic_model.values.VariableValue
import logger.Severity
import logger.issues.access.InstanceAccessFromStaticContext
import logger.issues.access.StaticAccessFromInstanceContext
import logger.issues.resolution.NotCallable
import logger.issues.resolution.NotFound
import logger.issues.resolution.SignatureAmbiguity
import logger.issues.resolution.SignatureMismatch
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
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
		assertNotNull(initializerCall?.providedType)
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
		assertNotNull(initializerCall?.providedType)
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
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<SignatureAmbiguity>("""
			Call to initializer '<Int>List(Int)' is ambiguous. Matching signatures:
			 - '<Element>List(Int)' declared at Test.Test:5:1
			 - '<Element>List(Element)' declared at Test.Test:6:1
		""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `resolves non-variadic initializer calls with plural type`() {
		val sourceCode =
			"""
				Int class
				IntegerList class {
					init(...integers: ...Int)
				}
				IntegerList()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function as? VariableValue)?.name == "IntegerList"
		}
		assertNotNull(initializerCall?.providedType)
	}

	@Test
	fun `resolves variadic initializer calls without variadic parameters`() {
		val sourceCode =
			"""
				Int class
				IntegerList class {
					init(capacity: Int, ...integers: ...Int)
				}
				IntegerList(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function as? VariableValue)?.name == "IntegerList"
		}
		assertNotNull(initializerCall?.providedType)
	}

	@Test
	fun `resolves variadic initializer calls with one variadic parameter`() {
		val sourceCode =
			"""
				Int class
				IntegerList class {
					init(capacity: Int, ...integers: ...Int)
				}
				IntegerList(Int(), Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function as? VariableValue)?.name == "IntegerList"
		}
		assertNotNull(initializerCall?.providedType)
	}

	@Test
	fun `resolves variadic initializer calls with multiple variadic parameters`() {
		val sourceCode =
			"""
				Int class
				IntegerList class {
					init(capacity: Int, ...integers: ...Int)
				}
				IntegerList(Int(), Int(), Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function as? VariableValue)?.name == "IntegerList"
		}
		assertNotNull(initializerCall?.providedType)
	}

	@Test
	fun `resolves the most specific signature between parameters`() {
		val sourceCode =
			"""
				Int class
				Number class
				Bottle class {
					init(volume: Number)
					init(volume: Int)
				}
				Bottle(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function as? VariableValue)?.name == "Bottle"
		}
		assertEquals("Bottle(Int)", initializerCall?.targetInitializer.toString())
	}

	@Test
	fun `resolves the most specific signature ignoring extraneous variadic parameter`() {
		val sourceCode =
			"""
				Int class
				Bottle class {
					init(volume: Int)
					init(volume: Int, ...idBytes: ...Int)
				}
				Bottle(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function as? VariableValue)?.name == "Bottle"
		}
		assertEquals("Bottle(Int)", initializerCall?.targetInitializer.toString())
	}

	@Test
	fun `resolves the most specific signature between variadic parameters`() {
		val sourceCode =
			"""
				Int class
				Number class
				Bottle class {
					init(...idBytes: ...Number)
					init(...idBytes: ...Int)
				}
				Bottle(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function as? VariableValue)?.name == "Bottle"
		}
		assertEquals("Bottle(...Int)", initializerCall?.targetInitializer.toString())
	}

	@Test
	fun `resolves the most specific signature considering the number of required conversions`() {
		val sourceCode =
			"""
				Cup class
				Bowl class {
					converting init(cup: Cup)
				}
				Bottle class {
					init(cup: Cup)
					init(bowl: Bowl)
				}
				Bottle(Cup())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function as? VariableValue)?.name == "Bottle"
		}
		assertEquals("Bottle(Cup)", initializerCall?.targetInitializer.toString())
	}

	@Test
	fun `resolves the most specific signature considering whether the target initializer is converting`() {
		val sourceCode =
			"""
				Glass class
				Bottle class {
					converting init(glass: Glass)
					init(other: Bottle)
				}
				GlassBottle object: Glass & Bottle
				Bottle(GlassBottle)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall> { functionCall ->
			(functionCall.function as? VariableValue)?.name == "Bottle"
		}
		assertEquals("Bottle(Bottle)", initializerCall?.targetInitializer?.toString())
	}

	@Test
	fun `converts supplied parameters to match signature if possible`() {
		val sourceCode =
			"""
				Int class
				Float class {
					converting init(value: Int)
				}
				Bottle class {
					val height: Float
					init(height)
				}
				Bottle(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotFound>()
	}

	@Test
	fun `resolves initializer when target type is a type alias`() {
		val sourceCode =
			"""
				Int class
				alias Integer = Int
				Integer()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotFound>()
		lintResult.assertIssueNotDetected<SignatureMismatch>()
		lintResult.assertIssueNotDetected<NotCallable>()
	}

	@Test
	fun `resolves initializer when parameter type is a type alias`() {
		val sourceCode =
			"""
				Int class {
					init
					init(template: Int)
				}
				alias Integer = Int
				val integer: Integer = Int()
				Int(integer)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotFound>()
		lintResult.assertIssueNotDetected<SignatureMismatch>()
	}

	@Test
	fun `conversions take precedence over variadic-ness in disambiguation`() {
		val sourceCode =
			"""
				Int class
				Byte class {
					converting init(integer: Int)
				}
				Container class {
					init(...integers: ...Int)
					init(byte: Byte)
				}
				Container(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall>()
		assertEquals("Container(...Int)", initializerCall?.targetInitializer.toString())
	}
}
