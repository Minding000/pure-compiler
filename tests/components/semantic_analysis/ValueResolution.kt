package components.semantic_analysis

import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.operations.IndexAccess
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.values.VariableValue
import messages.Message
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull

internal class ValueResolution {

	@Test
	fun `emits error for undeclared variables`() {
		val sourceCode =
			"""
				numberOfDogs
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Value 'numberOfDogs' hasn't been declared yet")
	}

	@Test
	fun `resolves local variables`() {
		val sourceCode =
			"""
				val numberOfCats = 2
				numberOfCats
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
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
		val lintResult = TestUtil.lint(sourceCode)
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
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "speed" }
		assertNotNull(variableValue?.definition)
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
		val functionType = variableValue?.type?.scope?.resolveValue("open")?.type as? FunctionType
		assertNotNull(functionType)
		val signature = functionType.resolveSignature()
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
		val lintResult = TestUtil.lint(sourceCode)
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
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val functionType = variableValue?.type?.scope?.resolveValue("open")?.type as? FunctionType
		assertNotNull(functionType)
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
		val functionType = variableValue?.type?.scope?.resolveValue("open")?.type as? FunctionType
		assertNotNull(functionType)
		val signature = functionType.resolveSignature()
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
	fun `emits error for undeclared unary operators`() {
		val sourceCode =
			"""
				val a = 5
				!a
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Operator '!Int' hasn't been declared yet")
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
		val lintResult = TestUtil.lint(sourceCode)
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
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Operator 'Matrix - Matrix' hasn't been declared yet")
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
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "a" }
		val operator = variableValue?.type?.scope?.resolveOperator("+", variableValue)
		assertNotNull(operator)
	}

	@Test
	fun `emits error for call to nonexistent index operator`() {
		val sourceCode =
			"""
				class Position {
					init
				}
				object ChessBoard {}
				val firstField = ChessBoard[Position()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Operator 'ChessBoard[Position]()' hasn't been declared yet.")
	}

	@Test
	fun `emits error for assignment to readonly index operator`() {
		val sourceCode =
			"""
				class Position {
					init
				}
				class Field {
					init
				}
				object ChessBoard {
					native operator[position: Position](): Field
				}
				ChessBoard[Position()] = Field()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Operator 'ChessBoard[Position](Field)' hasn't been declared yet.")
	}

	@Test
	fun `resolves index operators`() {
		val sourceCode =
			"""
				class Position {
					init
				}
				class Field {}
				object ChessBoard {
					native operator[position: Position](): Field
				}
				ChessBoard[Position()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val indexAccess = lintResult.find<IndexAccess>()
		assertNotNull(indexAccess?.type)
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
		val lintResult = TestUtil.lint(sourceCode)
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
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "The provided parameters (Bright) don't match any signature of function 'Light.shine'")
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

	@Disabled
	@Test
	fun `resolves operator calls with a variable number of parameters`() {
		val sourceCode =
			"""
				native class Int {
					init
				}
				object IntegerList {
					operator +=(...integers: ...Int) {}
				}
				IntegerList += Int(), Int()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall>()
		assertNotNull(initializerCall?.type)
	}
}
