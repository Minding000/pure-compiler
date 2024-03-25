package components.semantic_model.types

import components.semantic_model.values.Function
import logger.issues.constant_conditions.TypeNotAssignable
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
		assertEquals("=>|", function?.providedType.toString())
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
		assertEquals("=>| & (Int) =>|", function?.providedType.toString())
	}

	@Test
	fun `can be assigned to itself`() {
		val sourceCode =
			"""
				Tree object {
					to grow()
				}
				val growthFunction: =>| = Tree.grow
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Disabled
	@Test
	fun `can not be assigned to a type input wise narrower function type`() {
		val sourceCode =
			"""
				Paper class
				Letter class: Paper
				Stamper object {
					to stamp(L: Letter; letter: L): L
				}
				val stampFunction: (P: Paper; P) => P = Stamper.stamp
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<TypeNotAssignable>(
			"Type '(L: Letter; L) => L' is not assignable to type '(P: Paper; P) => P'.")
	}

	@Test
	fun `can not be assigned to a value input wise narrower function type`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				Tree object {
					to grow(amount: Int)
				}
				val growthFunction: (Number) =>| = Tree.grow
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<TypeNotAssignable>(
			"Type '(Int) =>|' is not assignable to type '(Number) =>|'.")
	}

	@Test
	fun `can not be assigned to a value output wise narrower function type`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				CashMachine object {
					to getBalance(): Number
				}
				val growthFunction: => Int = CashMachine.getBalance
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<TypeNotAssignable>(
			"Type '=> Number' is not assignable to type '=> Int'.")
	}

	@Disabled
	@Test
	fun `can be assigned to a type input wise broader function type`() {
		val sourceCode =
			"""
				Paper class
				Letter class: Paper
				Stamper object {
					to stamp(P: Paper; paper: P): P
				}
				val stampFunction: (L: Letter; L) => L = Stamper.stamp
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `can be assigned to a value input wise boarder function type`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				Tree object {
					to grow(amount: Number)
				}
				val growthFunction: (Int) =>| = Tree.grow
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `can be assigned to a value output wise boarder function type`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				CashMachine object {
					to getBalance(): Int
				}
				val growthFunction: => Number = CashMachine.getBalance
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `can be assigned to a function type with more signatures`() {
		val sourceCode =
			"""
				Body class
				Error class
				DataModel object {
					to process(result: Body | Error) {}
				}
				val resultProcessor: (Body) =>| & (Error) =>| = DataModel.process
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `can be assigned to an any object type`() {
		val sourceCode =
			"""
				DataModel object {
					to process() {}
				}
				val resultProcessor: Any = DataModel.process
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}
}
