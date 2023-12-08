package components.semantic_model.resolution

import components.semantic_model.operations.BinaryModification
import components.semantic_model.operations.IndexAccess
import components.semantic_model.values.Operator
import components.semantic_model.values.VariableValue
import logger.Severity
import logger.issues.access.WhereClauseUnfulfilled
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.modifiers.MissingOverridingKeyword
import logger.issues.modifiers.OverriddenSuperMissing
import logger.issues.modifiers.OverridingPropertyTypeMismatch
import logger.issues.modifiers.OverridingPropertyTypeNotAssignable
import logger.issues.resolution.NotFound
import logger.issues.resolution.SignatureAmbiguity
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
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
		lintResult.assertIssueDetected<NotFound>("Operator '!Int' hasn't been declared yet.")
	}

	@Test
	fun `resolves unary operator calls`() {
		val sourceCode =
			"""
				Fraction class {
					operator -
				}
				val fraction = Fraction()
				-fraction
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotFound>()
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "fraction" }
		val operator = variableValue?.type?.interfaceScope?.getOperator(Operator.Kind.MINUS)
		assertNotNull(operator)
	}

	@Test
	fun `emits error for undeclared binary operators`() {
		val sourceCode =
			"""
				Matrix class
				val {
					a = Matrix()
					b = Matrix()
				}
				var c = a - b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<NotFound>("Operator 'Matrix - Matrix' hasn't been declared yet.")
	}

	@Test
	fun `resolves binary operator calls`() {
		val sourceCode =
			"""
				Matrix class {
					operator +(other: Matrix): Matrix
				}
				val {
					a = Matrix()
					b = Matrix()
				}
				var c = a + b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotFound>()
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "a" }
		val operator = variableValue?.type?.interfaceScope?.getOperator(Operator.Kind.PLUS, variableValue)
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
		lintResult.assertIssueDetected<NotFound>("Operator 'Int--' hasn't been declared yet.")
	}

	@Test
	fun `resolves unary modification operator calls`() {
		val sourceCode =
			"""
				Fraction class {
					operator --
				}
				val fraction = Fraction()
				fraction--
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotFound>()
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "fraction" }
		val operator = variableValue?.type?.interfaceScope?.getOperator(Operator.Kind.DOUBLE_MINUS)
		assertNotNull(operator)
	}

	@Test
	fun `emits error for undeclared binary modification operators`() {
		val sourceCode =
			"""
				Matrix class
				val {
					a = Matrix()
					b = Matrix()
				}
				a += b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<NotFound>("Operator 'Matrix += Matrix' hasn't been declared yet.")
	}

	@Test
	fun `resolves binary modification operator calls`() {
		val sourceCode =
			"""
				Matrix class {
					operator +=(other: Matrix): Matrix
				}
				val {
					a = Matrix()
					b = Matrix()
				}
				a += b
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotFound>()
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "a" }
		val operator = variableValue?.type?.interfaceScope?.getOperator(Operator.Kind.PLUS_EQUALS, variableValue)
		assertNotNull(operator)
	}

	@Test
	fun `emits error for call to nonexistent index operator`() {
		val sourceCode =
			"""
				Position class
				ChessBoard object
				val firstField = ChessBoard[Position()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<NotFound>("Operator 'ChessBoard[Position]()' hasn't been declared yet.")
	}

	@Test
	fun `emits error for assignment to readonly index operator`() {
		val sourceCode =
			"""
				Position class
				Field class
				ChessBoard object {
					native operator[position: Position](): Field
				}
				ChessBoard[Position()] = Field()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<NotFound>("Operator 'ChessBoard[Position](Field)' hasn't been declared yet.")
	}

	@Test
	fun `resolves get index operators`() {
		val sourceCode =
			"""
				Position class
				Field class
				ChessBoard object {
					native operator[position: Position](): Field
				}
				ChessBoard[Position()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotFound>()
		val indexAccess = lintResult.find<IndexAccess>()
		assertNotNull(indexAccess?.type)
	}

	@Test
	fun `resolves set index operators`() {
		val sourceCode =
			"""
				Position class
				Field class
				ChessBoard object {
					native operator[position: Position](field: Field)
				}
				ChessBoard[Position()] = Field()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NotFound>()
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
		val indexAccess = lintResult.find<IndexAccess>()
		assertNotNull(indexAccess?.type)
	}

	@Test
	fun `resolves calls to super operator`() {
		val sourceCode =
			"""
				Int class
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
		lintResult.assertIssueNotDetected<OverridingPropertyTypeNotAssignable>()
		lintResult.assertIssueNotDetected<OverridingPropertyTypeMismatch>()
		val indexAccess = lintResult.find<IndexAccess>()
		assertNotNull(indexAccess?.type)
	}

	@Test
	fun `resolves calls to overriding operator`() {
		val sourceCode =
			"""
				Int class
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
		lintResult.assertIssueDetected<MissingOverridingKeyword>(
			"Operator 'VegetableShoppingList[Int]: Int' is missing the 'overriding' keyword.")
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
		lintResult.assertIssueNotDetected<MissingOverridingKeyword>()
		lintResult.assertIssueNotDetected<OverriddenSuperMissing>()
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
		lintResult.assertIssueDetected<OverriddenSuperMissing>(
			"'overriding' keyword is used, but the operator doesn't have a super operator.", Severity.WARNING)
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
		lintResult.assertIssueDetected<NotFound>("Operator 'List[Bright]()' hasn't been declared yet.", Severity.ERROR)
	}

	@Test
	fun `emits error for ambiguous operator calls`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				Boolean class
				List class {
					containing Element
					operator [index: Int]: Element
					operator [element: Element]: Boolean
					operator [N: Number; index: N]: Boolean
				}
				val numbers = <Int>List()
				numbers[Int()]
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<SignatureAmbiguity>("""
			Call to operator '<Int>List[Int]' is ambiguous. Matching signatures:
			 - '(Int) => Element' declared at Test.Test:6:10
			 - '(Element) => Boolean' declared at Test.Test:7:10
			 - '(N: Number; N) => Boolean' declared at Test.Test:8:10
		""".trimIndent())
	}

	@Test
	fun `resolves the most specific signature between parameters`() {
		val sourceCode =
			"""
				Int class
				Number class
				Bottle object {
					operator +=(volume: Number)
					operator +=(volume: Int)
				}
				Bottle += Int()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val operatorCall = lintResult.find<BinaryModification>()
		assertEquals("(Int) =>|", operatorCall?.targetSignature.toString())
	}

	@Test
	fun `allows accessing operators without where clause`() {
		val sourceCode = """
			abstract Addable class
			IntegerList class {
				operator []: Addable
			}
			Int class: Addable
			val integerList = IntegerList()
			integerList[]
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<WhereClauseUnfulfilled>()
	}

	@Test
	fun `allows accessing operators with where clause when type condition is met`() {
		val sourceCode = """
			ID class
			String class
			Map class {
				containing Key, Value
				operator [] where Value is String and Key is ID {}
			}
			val stringMap = <ID, String>Map()
			stringMap[]
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueNotDetected<WhereClauseUnfulfilled>()
	}

	@Test
	fun `disallows accessing index operators with where clause when type condition is not met`() {
		val sourceCode = """
			abstract Addable class
			List class {
				containing Element
				operator []: Element where Element is specific Addable
			}
			Parachute class
			val parachuteList = <Parachute>List()
			parachuteList[]
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<WhereClauseUnfulfilled>(
			"Operator '[]: Element' cannot be accessed on object of type '<Parachute>List'," +
				" because the condition 'Element is specific Addable' is not met.", Severity.ERROR)
	}

	@Test
	fun `disallows accessing unary operators with where clause when type condition is not met`() {
		val sourceCode = """
			abstract Addable class
			List class {
				containing Element
				operator !: Element where Element is specific Addable
			}
			Parachute class
			val parachuteList = <Parachute>List()
			!parachuteList
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<WhereClauseUnfulfilled>(
			"Operator '!: Element' cannot be accessed on object of type '<Parachute>List'," +
				" because the condition 'Element is specific Addable' is not met.", Severity.ERROR)
	}

	@Test
	fun `disallows accessing unary modifications with where clause when type condition is not met`() {
		val sourceCode = """
			abstract Addable class
			List class {
				containing Element
				operator ++: Element where Element is specific Addable
			}
			Parachute class
			val parachuteList = <Parachute>List()
			parachuteList++
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<WhereClauseUnfulfilled>(
			"Operator '++' cannot be accessed on object of type '<Parachute>List'," +
				" because the condition 'Element is specific Addable' is not met.", Severity.ERROR)
	}

	@Test
	fun `disallows accessing binary operators with where clause when type condition is not met`() {
		val sourceCode = """
			abstract Addable class
			List class {
				containing Element
				operator +(other: <Element>List): Element where Element is specific Addable
			}
			Parachute class
			val parachuteList = <Parachute>List()
			parachuteList + parachuteList
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<WhereClauseUnfulfilled>(
			"Operator ' + <Element>List: Element' cannot be accessed on object of type '<Parachute>List'," +
				" because the condition 'Element is specific Addable' is not met.", Severity.ERROR)
	}

	@Test
	fun `disallows accessing binary modification with where clause when type condition is not met`() {
		val sourceCode = """
			abstract Addable class
			List class {
				containing Element
				operator +=(other: <Element>List): Element where Element is specific Addable
			}
			Parachute class
			val parachuteList = <Parachute>List()
			parachuteList += parachuteList
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<WhereClauseUnfulfilled>(
			"Operator ' += <Element>List' cannot be accessed on object of type '<Parachute>List'," +
				" because the condition 'Element is specific Addable' is not met.", Severity.ERROR)
	}

	@Test
	fun `allows accessing inherited operators with where clause when type condition is met`() {
		val sourceCode = """
			ID class
			String class
			Map class {
				containing Key, Value
				operator [] where Value is String and Key is ID {}
			}
			InvertedMap class: <Value, Key>Map {
				containing Key, Value
			}
			val stringMap = <ID, String>Map()
			stringMap[]
			val invertedStringMap = <String, ID>InvertedMap()
			invertedStringMap[]
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueNotDetected<WhereClauseUnfulfilled>()
	}

	@Test
	fun `disallows accessing inherited operators with where clause when type condition is not met`() {
		val sourceCode = """
			abstract Addable class
			List class {
				containing Element
				operator []: Element where Element is specific Addable
			}
			LinkedList class: <Element>List {
				containing Element
			}
			Parachute class
			val parachuteList = <Parachute>LinkedList()
			parachuteList[]
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<WhereClauseUnfulfilled>(
			"Operator '[]: Element' cannot be accessed on object of type '<Parachute>LinkedList'," +
				" because the condition 'Element is specific Addable' is not met.", Severity.ERROR)
	}

	@Test
	fun `allows accessing inherited operators with statically fulfilled where clause`() {
		val sourceCode = """
			abstract Addable class
			List class {
				containing Element
				operator []: Element where Element is specific Addable
			}
			Int class: Addable
			IntegerList class: <Int>List
			val integerList = IntegerList()
			integerList[]
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<WhereClauseUnfulfilled>()
	}

	@Test
	fun `disallows accessing inherited operators with statically unfulfilled where clause`() {
		val sourceCode = """
			abstract Addable class
			List class {
				containing Element
				operator []: Element where Element is specific Addable
			}
			Parachute class
			ParachuteList class: <Parachute>List
			val parachuteList = ParachuteList()
			parachuteList[]
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueDetected<WhereClauseUnfulfilled>(
			"Operator '[]: Element' cannot be accessed on object of type 'ParachuteList'," +
				" because the condition 'Element is specific Addable' is not met.", Severity.ERROR)
	}
}
