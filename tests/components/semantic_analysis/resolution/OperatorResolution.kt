package components.semantic_analysis.resolution

import components.semantic_analysis.semantic_model.control_flow.FunctionCall
import components.semantic_analysis.semantic_model.operations.IndexAccess
import components.semantic_analysis.semantic_model.values.Operator
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
				Fraction class {
					init
					operator -
				}
				val fraction = Fraction()
				-fraction
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "fraction" }
		val operator = variableValue?.type?.scope?.resolveOperator(Operator.Kind.MINUS)
		assertNotNull(operator)
	}

	@Test
	fun `emits error for undeclared binary operators`() {
		val sourceCode =
			"""
				Matrix class {
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
				Matrix class {
					init
					operator +(other: Matrix): Matrix
				}
				val {
					a = Matrix()
					b = Matrix()
				}
				var c = a + b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "a" }
		val operator = variableValue?.type?.scope?.resolveOperator(Operator.Kind.PLUS, variableValue)
		assertNotNull(operator)
	}

	@Test
	fun `emits error for undeclared unary modification operators`() {
		val sourceCode =
			"""
				val a = 5
				a--
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Operator 'Int--' hasn't been declared yet")
	}

	@Test
	fun `resolves unary modification operator calls`() {
		val sourceCode =
			"""
				Fraction class {
					init
					operator --
				}
				val fraction = Fraction()
				fraction--
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "fraction" }
		val operator = variableValue?.type?.scope?.resolveOperator(Operator.Kind.DOUBLE_MINUS)
		assertNotNull(operator)
	}

	@Test
	fun `emits error for undeclared binary modification operators`() {
		val sourceCode =
			"""
				Matrix class {
					init
				}
				val {
					a = Matrix()
					b = Matrix()
				}
				a += b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Operator 'Matrix += Matrix' hasn't been declared yet")
	}

	@Test
	fun `resolves binary modification operator calls`() {
		val sourceCode =
			"""
				Matrix class {
					init
					operator +=(other: Matrix): Matrix
				}
				val {
					a = Matrix()
					b = Matrix()
				}
				a += b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "a" }
		val operator = variableValue?.type?.scope?.resolveOperator(Operator.Kind.PLUS_EQUALS, variableValue)
		assertNotNull(operator)
	}

	@Test
	fun `emits error for call to nonexistent index operator`() {
		val sourceCode =
			"""
				Position class {
					init
				}
				ChessBoard object
				val firstField = ChessBoard[Position()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Operator 'ChessBoard[Position]()' hasn't been declared yet.")
	}

	@Test
	fun `emits error for assignment to readonly index operator`() {
		val sourceCode =
			"""
				Position class {
					init
				}
				Field class {
					init
				}
				ChessBoard object {
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
				Position class {
					init
				}
				Field class
				ChessBoard object {
					native operator[position: Position](): Field
				}
				ChessBoard[Position()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val indexAccess = lintResult.find<IndexAccess>()
		assertNotNull(indexAccess?.type)
	}

	@Test
	fun `resolves calls to super operator`() {
		val sourceCode =
			"""
				Int class {
					init
				}
				Hinge class
				Door class {
					operator [index: Int](): Hinge
				}
				TransparentDoor class: Door
				GlassDoor object: TransparentDoor {
					operator [index: Int](hinge: Hinge)
				}
				GlassDoor[Int()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val indexAccess = lintResult.find<IndexAccess>()
		assertNotNull(indexAccess?.type)
	}

	@Test
	fun `resolves calls to overriding operator`() {
		val sourceCode =
			"""
				Int class {
					init
				}
				Hinge class
				Door class {
					operator [index: Int]: Hinge
				}
				TransparentDoor class: Door
				GlassDoor object: TransparentDoor {
					overriding operator [index: Int]: Hinge
				}
				GlassDoor[Int()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val indexAccess = lintResult.find<IndexAccess>()
		assertNotNull(indexAccess?.type)
	}

	@Test
	fun `detects missing overriding keyword on operator`() {
		val sourceCode =
			"""
				Int class
				ShoppingList class {
					operator [index: Int](): Int
				}
				FoodShoppingList class: ShoppingList {
					operator [index: Int](foodId: Int)
				}
				VegetableShoppingList class: FoodShoppingList {
					operator [index: Int](): Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Missing 'overriding' keyword")
	}

	@Test
	fun `allows for operators to be overridden`() {
		val sourceCode =
			"""
				Int class
				ShoppingList class {
					operator [index: Int](): Int
				}
				FoodShoppingList class: ShoppingList
				VegetableShoppingList class: FoodShoppingList {
					overriding operator [index: Int](): Int
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
				Room class {
					overriding operator +()
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
				Int class
				Bright object
				List object {
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
				Int class {
					init
				}
				Boolean class
				List class {
					containing Element

					init

					operator [index: Int]: Element
					operator [element: Element]: Boolean
				}
				val numbers = <Int>List()
				numbers[Int()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Call to operator '<Int>List[Int]' is ambiguous")
	}

	@Disabled
	@Test
	fun `resolves operator calls with a variable number of parameters`() {
		val sourceCode =
			"""
				Int class {
					init
				}
				IntegerList object {
					operator +=(...integers: ...Int)
				}
				IntegerList += Int(), Int()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializerCall = lintResult.find<FunctionCall>()
		assertNotNull(initializerCall?.type)
	}
}
