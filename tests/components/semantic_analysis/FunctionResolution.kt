package components.semantic_analysis

import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.values.VariableValue
import messages.Message
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertIs
import kotlin.test.assertNotNull

internal class FunctionResolution {

	@Test
	fun `resolves function calls`() {
		val sourceCode =
			"""
				object Door {
					to open() {}
				}
				Door.open()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "Door" }
		val functionType = variableValue?.type?.scope?.resolveValue("open")?.type
		assertIs<FunctionType>(functionType)
		val signature = functionType.resolveSignature()
		assertNotNull(signature)
	}

	@Test
	fun `resolves calls to super function`() {
		val sourceCode =
			"""
				native class Speed {}
				class Door {
					to open() {}
				}
				class TransparentDoor: Door {}
				object GlassDoor: TransparentDoor {
					to open(speed: Speed) {}
				}
				GlassDoor.open()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val functionType = variableValue?.type?.scope?.resolveValue("open")?.type
		assertIs<FunctionType>(functionType)
		val signature = functionType.resolveSignature()
		assertNotNull(signature)
	}

	@Test
	fun `resolves calls to overriding function`() {
		val sourceCode =
			"""
				class Door {
					to open() {}
				}
				class TransparentDoor: Door {}
				object GlassDoor: TransparentDoor {
					overriding to open() {}
				}
				GlassDoor.open()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val functionType = variableValue?.type?.scope?.resolveValue("open")?.type
		assertIs<FunctionType>(functionType)
		val signature = functionType.resolveSignature()
		assertNotNull(signature)
	}

	@Test
	fun `detects missing overriding keyword on function`() {
		val sourceCode =
			"""
				class Food {
					to check() {}
				}
				class Vegetable: Food {
					to check(): Vegetable {}
				}
				class Potato: Vegetable {
					to check() {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Missing 'overriding' keyword")
	}

	@Test
	fun `allows for functions to be overridden`() {
		val sourceCode =
			"""
				class Food {
					to check() {}
				}
				class Vegetable: Food {}
				class Potato: Vegetable {
					overriding to check() {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Missing 'overriding' keyword")
		lintResult.assertMessageNotEmitted(Message.Type.WARNING,
			"'overriding' keyword is used, but the function doesn't have a super function")
	}

	@Test
	fun `detects overriding keyword being used without super function`() {
		val sourceCode =
			"""
				class Room {
					overriding to clean() {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"'overriding' keyword is used, but the function doesn't have a super function")
	}

	@Test
	fun `emits error for function calls with wrong parameters`() {
		val sourceCode =
			"""
				object Bright {}
				object Light {
					to shine() {}
				}
				Light.shine(Bright)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"The provided parameters (Bright) don't match any signature of function 'Light.shine'")
	}

	@Test
	fun `emits error for ambiguous function calls`() {
		val sourceCode =
			"""
				class Int {
					init
				}
				class List {
					containing Element

					init

					it exists(index: Int) {}
					it exists(element: Element) {}
				}
				val numbers = <Int>List()
				numbers.exists(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Call to function '<Int>List.exists(Int)' is ambiguous")
	}

	@Disabled
	@Test
	fun `resolves function calls with a variable number of parameters`() {
		val sourceCode =
			"""
				native class Int {
					init
				}
				object IntegerList {
					to add(...integers: ...Int) {}
				}
				IntegerList.add(Int())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall>()
		assertNotNull(initializerCall?.type)
	}
}
