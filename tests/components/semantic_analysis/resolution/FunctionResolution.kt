package components.semantic_analysis.resolution

import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.operations.MemberAccess
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.Severity
import logger.issues.modifiers.MissingOverridingKeyword
import logger.issues.modifiers.OverriddenSuperMissing
import logger.issues.modifiers.OverridingPropertyTypeMismatch
import logger.issues.modifiers.OverridingPropertyTypeNotAssignable
import logger.issues.resolution.SignatureAmbiguity
import logger.issues.resolution.SignatureMismatch
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertIs
import kotlin.test.assertNotNull

internal class FunctionResolution {

	@Test
	fun `resolves function calls`() {
		val sourceCode =
			"""
				Door object {
					to open()
				}
				Door.open()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "Door" }
		val functionType = variableValue?.type?.interfaceScope?.resolveValue("open")?.type
		assertIs<FunctionType>(functionType)
		val signature = functionType.resolveSignature()
		assertNotNull(signature)
	}

	@Test
	fun `resolves calls to super function`() {
		val sourceCode =
			"""
				Speed class
				Door class {
					to open()
				}
				TransparentDoor class: Door
				GlassDoor object: TransparentDoor {
					to open(speed: Speed)
				}
				GlassDoor.open()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OverridingPropertyTypeNotAssignable>()
		lintResult.assertIssueNotDetected<OverridingPropertyTypeMismatch>()
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val functionType = variableValue?.type?.interfaceScope?.resolveValue("open")?.type
		assertIs<FunctionType>(functionType)
		val signature = functionType.resolveSignature()
		assertNotNull(signature)
	}

	@Test
	fun `resolves calls to overriding function`() {
		val sourceCode =
			"""
				Door class {
					to open()
				}
				TransparentDoor class: Door
				GlassDoor object: TransparentDoor {
					overriding to open()
				}
				GlassDoor.open()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val functionType = variableValue?.type?.interfaceScope?.resolveValue("open")?.type
		assertIs<FunctionType>(functionType)
		val signature = functionType.resolveSignature()
		assertNotNull(signature)
	}

	@Test
	fun `detects missing overriding keyword on function`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					to getNutritionScore(): Number
				}
				Vegetable class: Food
				Potato class: Vegetable {
					to getNutritionScore(): Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingOverridingKeyword>(
			"Function 'Potato.getNutritionScore(): Float' is missing the 'overriding' keyword.")
	}

	@Test
	fun `allows for functions to be overridden`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					to getNutritionScore(): Number
				}
				Vegetable class: Food
				Potato class: Vegetable {
					overriding to getNutritionScore(): Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingOverridingKeyword>()
		lintResult.assertIssueNotDetected<OverriddenSuperMissing>()
	}

	@Test
	fun `detects overriding keyword being used without super function`() {
		val sourceCode =
			"""
				Room class {
					overriding to clean()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverriddenSuperMissing>(
			"'overriding' keyword is used, but the function doesn't have a super function.", Severity.WARNING)
	}

	@Test
	fun `emits error for function calls with wrong parameters`() {
		val sourceCode =
			"""
				Bright object
				Light object {
					to shine()
				}
				Light.shine(Bright)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<SignatureMismatch>(
			"The provided parameters (Bright) don't match any signature of function 'Light.shine'.", Severity.ERROR)
	}

	@Test
	fun `emits error for ambiguous function calls`() {
		val sourceCode =
			"""
				Int class
				List class {
					containing Element
					it exists(index: Int)
					it exists(element: Element)
				}
				val numbers = <Int>List()
				numbers.exists(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<SignatureAmbiguity>("""
			Call to function '<Int>List.exists(Int)' is ambiguous. Matching signatures:
			 - '(Int) =>|' declared at Test.Test:4:4
			 - '(Int) =>|' declared at Test.Test:5:4
		""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `resolves variadic function calls without variadic parameters`() {
		val sourceCode =
			"""
				Int class
				IntegerList object {
					to add(capacity: Int, ...integers: ...Int)
				}
				IntegerList.add(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertNotNull(functionCall?.type)
	}

	@Test
	fun `resolves variadic function calls with one variadic parameter`() {
		val sourceCode =
			"""
				Int class
				IntegerList object {
					to add(capacity: Int, ...integers: ...Int)
				}
				IntegerList.add(Int(), Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertNotNull(functionCall?.type)
	}

	@Test
	fun `resolves variadic function calls with multiple variadic parameters`() {
		val sourceCode =
			"""
				Int class
				IntegerList object {
					to add(capacity: Int, ...integers: ...Int)
				}
				IntegerList.add(Int(), Int(), Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertNotNull(functionCall?.type)
	}
}
