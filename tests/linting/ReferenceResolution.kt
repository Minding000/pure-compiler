package linting

import linting.semantic_model.literals.FunctionType
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.values.TypeDefinition
import linting.semantic_model.values.VariableValue
import util.TestUtil
import linting.messages.Message
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

internal class ReferenceResolution {

	@Test
	fun `emits error for undeclared variables`() {
		val sourceCode =
			"""
				numberOfDogs
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Value 'numberOfDogs' hasn't been declared yet")
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
				native class Int {}
				class Window {
					init(width: Int, height: Int) {}
				}
				Window(2, 2)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val intDefinition = lintResult.find<TypeDefinition> { typeDefinition -> typeDefinition.name == "Int" }
		assertNotNull(intDefinition)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "Window" }
		val initializer = variableValue?.type?.scope?.resolveInitializer(listOf(ObjectType(intDefinition), ObjectType(intDefinition)))
		assertNotNull(initializer)
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
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Initializer 'Item(Item)' hasn't been declared yet")
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
	fun `emits error for undeclared operators`() {
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
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Operator '-(Matrix)' hasn't been declared yet")
	}

	@Test
	fun `resolves operator calls`() {
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
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "'Bird.age' is not callable")
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
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "The provided values don't match any signature of function 'Light.shine'")
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
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Call to function 'List.exists(Int)' is ambiguous")
	}
}