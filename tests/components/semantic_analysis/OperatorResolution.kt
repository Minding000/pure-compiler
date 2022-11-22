package components.semantic_analysis

import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.operations.IndexAccess
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.StaticType
import components.semantic_analysis.semantic_model.values.VariableValue
import messages.Message
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull

internal class OperatorResolution {

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

	//TODO add check for index operator to have return type exclusive or parameters
	//TODO add check for binary operator to take one parameter

	@Test
	fun `resolves calls to super operator`() {
		val sourceCode =
			"""
				class Int {
					init
				}
				class Hinge {}
				class Door {
					operator [index: Int](): Hinge {}
				}
				class TransparentDoor: Door {}
				object GlassDoor: TransparentDoor {
					operator [index: Int](hinge: Hinge) {}
				}
				GlassDoor[Int()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val initializerCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function.type is StaticType }
		assertNotNull(initializerCall)
		val operatorDefinition = variableValue?.type?.scope?.resolveIndexOperator(listOf(), listOf(initializerCall), listOf())
		assertNotNull(operatorDefinition)
	}

	@Test
	fun `resolves calls to overriding operator`() {
		val sourceCode =
			"""
				class Int {
					init
				}
				class Hinge {}
				class Door {
					operator [index: Int](): Hinge {}
				}
				class TransparentDoor: Door {}
				object GlassDoor: TransparentDoor {
					overriding operator [index: Int](): Hinge {}
				}
				GlassDoor[Int()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val functionType = variableValue?.type?.scope?.resolveValue("open")?.type as? FunctionType
		assertNotNull(functionType)
		val signature = functionType.resolveSignature()
		assertNotNull(signature)
	}

	@Test
	fun `detects missing overriding keyword on operator`() {
		val sourceCode =
			"""
				class Int {}
				class ShoppingList {
					operator [index: Int](): Int {}
				}
				class FoodShoppingList: ShoppingList {
					operator [index: Int](foodId: Int) {}
				}
				class VegetableShoppingList: FoodShoppingList {
					operator [index: Int](): Int {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Missing 'overriding' keyword")
	}

	@Test
	fun `allows for operators to be overridden`() {
		val sourceCode =
			"""
				class Int {}
				class ShoppingList {
					operator [index: Int](): Int {}
				}
				class FoodShoppingList: ShoppingList {}
				class VegetableShoppingList: FoodShoppingList {
					overriding operator [index: Int](): Int {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Missing 'overriding' keyword")
		lintResult.assertMessageNotEmitted(Message.Type.WARNING,
			"'overriding' keyword is used, but the operator doesn't have a super operator")
	}

	@Test
	fun `detects overriding keyword being used without super operator`() {
		val sourceCode =
			"""
				class Room {
					overriding operator +() {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"'overriding' keyword is used, but the operator doesn't have a super operator")
	}

	@Test
	fun `emits error for operator calls with wrong parameters`() {
		val sourceCode =
			"""
				class Int {}
				object Bright {}
				object List {
					operator [key: Int]: Int
				}
				List[Bright]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Operator 'List[Bright]()' hasn't been declared yet")
	}

	@Test
	fun `emits error for ambiguous operator calls`() {
		val sourceCode =
			"""
				class Int {
					init
				}
				class Boolean {}
				class List {
					containing Element

					init

					operator [index: Int]: Element {}
					operator [element: Element]: Boolean {}
				}
				val numbers = <Int>List()
				numbers[Int()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Index access '<Int>List[Int]' is ambiguous")
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
