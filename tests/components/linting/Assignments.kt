package components.linting

import components.linting.semantic_model.operations.IndexAccess
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull

internal class Assignments {

	@Test
	fun `emits error for incompatible source expression type`() {
		val sourceCode =
			"""
				val a = 5
				var b = "I'm not a number"
				b = a
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Type 'Int' is not assignable to type 'String'.")
	}

	@Test
	fun `emits error for assignment to constant target variable`() {
		val sourceCode =
			"""
				val a = 5
				val b = 4
				b = a
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'b' cannot be reassigned, because it is constant.")
	}

	@Test
	fun `emits error for assignment to nonexistent index operator`() {
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
	fun `complex types can be assigned to type aliases`() {
		val sourceCode =
			"""
				class Event {}
				alias EventHandler = (Event) =>|
				var typeAliasValue: EventHandler
				val complexTypeValue: (Event) =>|
				typeAliasValue = complexTypeValue
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Type '(Event) =>|' is not assignable to type 'EventHandler'")
	}

	@Test
	fun `type aliases can be assigned to complex types`() {
		val sourceCode =
			"""
				class Event {}
				alias EventHandler = (Event) =>|
				val typeAliasValue: EventHandler
				var complexTypeValue: (Event) =>|
				complexTypeValue = typeAliasValue
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Type 'EventHandler' is not assignable to type '(Event) =>|'")
	}

	@Test
	fun `can be assigned to optional types`() {
		val sourceCode =
			"""
				class Car {
					init
				}
				val car: Car? = Car()
				car = null
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Type 'Null' is not assignable to type 'Car?'")
	}
}
