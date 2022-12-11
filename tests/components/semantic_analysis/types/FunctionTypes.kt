package components.semantic_analysis.types

import components.semantic_analysis.semantic_model.values.Function
import messages.Message
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class FunctionTypes {

	@Test
	fun `can represent the type of a single contained signature`() {
		val sourceCode =
			"""
				Car class {
					to drive() {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<Function>()
		assertEquals("=>|", function?.type.toString())
	}

	@Test
	fun `can represent the type of multiple contained signatures`() {
		val sourceCode =
			"""
				Car class {
					to drive() {}
					to drive(distance: Int) {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<Function>()
		assertEquals("=>| & (Int) =>|", function?.type.toString())
	}

	@Test
	fun `can be assigned to itself`() {
		val sourceCode =
			"""
				Tree class {
					to grow()
				}
				val growthFunction: =>| = Tree.grow
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}

	@Disabled
	@Test
	fun `can not be assigned to a type input wise narrower function type`() {
		val sourceCode =
			"""
				Paper class {}
				Letter class: Paper {}
				Stamper class {
					to stamp(L: Letter; letter: L): L
				}
				val stampFunction: (P: Paper; P) => P = Stamper.stamp
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Type '(L: Letter; L) => L' is not assignable to type '(P: Paper; P) => P'")
	}

	@Test
	fun `can not be assigned to a value input wise narrower function type`() {
		val sourceCode =
			"""
				Number class {}
				Int class: Number {}
				Tree class {
					to grow(amount: Int)
				}
				val growthFunction: (Number) =>| = Tree.grow
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Type '(Int) =>|' is not assignable to type '(Number) =>|'")
	}

	@Test
	fun `can not be assigned to a value output wise narrower function type`() {
		val sourceCode =
			"""
				Number class {}
				Int class: Number {}
				CashMachine class {
					to getBalance(): Number
				}
				val growthFunction: => Int = CashMachine.getBalance
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Type '=> Number' is not assignable to type '=> Int'")
	}

	@Disabled
	@Test
	fun `can be assigned to a type input wise broader function type`() {
		val sourceCode =
			"""
				Paper class {}
				Letter class: Paper {}
				Stamper class {
					to stamp(P: Paper; paper: P): P
				}
				val stampFunction: (L: Letter; L) => L = Stamper.stamp
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}

	@Test
	fun `can be assigned to a value input wise boarder function type`() {
		val sourceCode =
			"""
				Number class {}
				Int class: Number {}
				Tree class {
					to grow(amount: Number)
				}
				val growthFunction: (Int) =>| = Tree.grow
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}

	@Test
	fun `can be assigned to a value output wise boarder function type`() {
		val sourceCode =
			"""
				Number class {}
				Int class: Number {}
				CashMachine class {
					to getBalance(): Int
				}
				val growthFunction: => Number = CashMachine.getBalance
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}

	@Test
	fun `can be assigned to a function type with more signatures`() {
		val sourceCode =
			"""
				Body class {}
				Error class {}
				DataModel class {
					to process(result: Body | Error) {}
				}
				val resultProcessor: (Body) =>| & (Error) =>| = DataModel.process
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}

	@Test
	fun `can be assigned to an any object type`() {
		val sourceCode =
			"""
				DataModel class {
					to process() {}
				}
				val resultProcessor: Any = DataModel.process
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}
}
