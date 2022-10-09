package linting

import linting.semantic_model.control_flow.FunctionCall
import linting.semantic_model.literals.FunctionType
import linting.semantic_model.values.VariableValue
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull

internal class ReferenceResolution {

	@Test
	fun `emits error for undeclared variables`() {
		val sourceCode =
			"""
				numberOfDogs
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Value 'numberOfDogs' hasn't been declared yet")
	}

	@Test
	fun `resolves local variables`() {
		val sourceCode =
			"""
				val numberOfCats = 2
				numberOfCats
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "numberOfCats" }
		assertNotNull(variableValue?.definition)
	}

	@Test
	fun `resolves instance members`() {
		val sourceCode =
			"""
				object House {
					val livingAreaInSquareMeters = 120
				}
				House.livingAreaInSquareMeters
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "livingAreaInSquareMeters" }
		assertNotNull(variableValue?.definition)
	}

	@Test
	fun `resolves parameters`() {
		val sourceCode =
			"""
				object House {
					to openDoor(speed: Int) {
						speed
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "speed" }
		assertNotNull(variableValue?.definition)
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
		val lintResult = TestUtil.lint(sourceCode, false)
		val initializerCall = lintResult.find<FunctionCall>()
		assertNotNull(initializerCall?.type)
	}

	@Test
	fun `emits error for undeclared initializers`() {
		val sourceCode =
			"""
				class Item {
					init() {}
				}
				Item(Item())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Initializer 'Item(Item)' hasn't been declared yet")
	}

	@Test
	fun `resolves function calls`() {
		val sourceCode =
			"""
				object Door {
					to open() {}
				}
				Door.open()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "Door" }
		val functionType = variableValue?.type?.scope?.resolveValue("open")?.type as? FunctionType
		assertNotNull(functionType)
		val signature = functionType.resolveSignature(listOf())
		assertNotNull(signature)
	}

	@Test
	fun `resolves super members`() {
		val sourceCode =
			"""
				class Door {
					val isOpen = yes
				}
				object GlassDoor: Door {
				}
				GlassDoor.isOpen
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val member = variableValue?.type?.scope?.resolveValue("isOpen")
		assertNotNull(member)
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
		val lintResult = TestUtil.lint(sourceCode, false)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val functionType = variableValue?.type?.scope?.resolveValue("open")?.type as? FunctionType
		assertNotNull(functionType)
		val signature = functionType.resolveSignature(listOf())
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
		val lintResult = TestUtil.lint(sourceCode, false)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val functionType = variableValue?.type?.scope?.resolveValue("open")?.type as? FunctionType
		assertNotNull(functionType)
		val signature = functionType.resolveSignature(listOf())
		assertNotNull(signature)
	}

	@Test
	fun `detects missing overriding keyword`() {
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
		val lintResult = TestUtil.lint(sourceCode, false)
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
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Missing 'overriding' keyword")
	}

	@Test
	fun `detects overriding keyword being used without super function`() {
		val sourceCode =
			"""
				class Room {
					overriding to clean() {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"'overriding' keyword is used, but the function doesn't have a super function")
	}

	@Test
	fun `emits error for undeclared unary operators`() {
		val sourceCode =
			"""
				val a = 5
				!a
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Operator '!()' hasn't been declared yet")
	}

	@Test
	fun `resolves unary operator calls`() {
		val sourceCode =
			"""
				class Fraction {
					init
					operator -() {}
				}
				val fraction = Fraction()
				-fraction
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "fraction" }
		val operator = variableValue?.type?.scope?.resolveOperator("-")
		assertNotNull(operator)
	}

	@Test
	fun `emits error for undeclared binary operators`() {
		val sourceCode =
			"""
				class Matrix {
					init
				}
				val {
					a = Matrix()
					b = Matrix()
				}
				var c = a - b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Operator '-(Matrix)' hasn't been declared yet")
	}

	@Test
	fun `resolves binary operator calls`() {
		val sourceCode =
			"""
				class Matrix {
					init
					operator +(other: Matrix): Matrix {}
				}
				val {
					a = Matrix()
					b = Matrix()
				}
				var c = a + b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "a" }
		val operator = variableValue?.type?.scope?.resolveOperator("+", variableValue.type)
		assertNotNull(operator)
	}

	@Test
	fun `emits error for calls to uncallable value`() {
		val sourceCode =
			"""
				object Bird {
					var age = 0
				}
				Bird.age()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'Bird.age' is not callable")
	}

	@Test
	fun `emits error for calls with wrong parameters`() {
		val sourceCode =
			"""
				object Bright {}
				object Light {
					to shine() {}
				}
				Light.shine(Bright)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "The provided values don't match any signature of function 'Light.shine'")
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
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Call to function 'List.exists(Int)' is ambiguous")
	}
}