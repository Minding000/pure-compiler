package components.semantic_analysis.declarations

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class OperatorSignatureValidation {

	@Test
	fun `emits warning for index operators with parameter and return type`() {
		val sourceCode = """
			Vector class {
				operator [key: IndexType](value: Int): Int
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"Index operators can not accept and return a value at the same time")
	}

	@Test
	fun `doesn't emit warning for index operators with either parameter or return type`() {
		val sourceCode = """
			Vector class {
				operator [key: IndexType](): Int
				operator [key: IndexType](value: Int)
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING,
			"Index operators can not accept and return a value at the same time")
	}

	@Test
	fun `emits warning for binary operators that don't take exactly one parameter`() {
		val sourceCode = """
			Vector class {
				operator -(a: Self, b: Self): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Binary operators need to accept exactly one parameter")
	}

	@Test
	fun `doesn't emit warning for binary operators that take exactly one parameter`() {
		val sourceCode = """
			Vector class {
				operator -(other: Self): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Binary operators need to accept exactly one parameter")
	}

	@Test
	fun `doesn't emit warning for unary operators that don't take exactly one parameter`() {
		val sourceCode = """
			Vector class {
				operator -(): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Binary operators need to accept exactly one parameter")
	}

	@Test
	fun `emits warning for unary operators that take parameters`() {
		val sourceCode = """
			Vector class {
				operator !(other: Self): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Unary operators can't accept parameters")
	}

	@Test
	fun `doesn't emit warning for unary operators that don't take parameters`() {
		val sourceCode = """
			Vector class {
				operator !(): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Unary operators can't accept parameters")
	}

	@Test
	fun `doesn't emit warning for binary operators that take parameters`() {
		val sourceCode = """
			Vector class {
				operator -(a: Self): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Unary operators can't accept parameters")
	}

	@Test
	fun `emits warning for returning operators that don't have a return type`() {
		val sourceCode = """
			Vector class {
				operator !()
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "This operator is expected to return a value")
	}

	@Test
	fun `doesn't emit warning for returning operators that have a return type`() {
		val sourceCode = """
			Vector class {
				operator !(): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "This operator is expected to return a value")
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "This operator is not expected to return a value")
	}

	@Test
	fun `emits warning for non-returning operators that have a return type`() {
		val sourceCode = """
			Vector class {
				operator ++(): Vector
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "This operator is not expected to return a value")
	}

	@Test
	fun `doesn't emit warning for non-returning operators that don't have a return type`() {
		val sourceCode = """
			Vector class {
				operator ++()
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "This operator is expected to return a value")
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "This operator is not expected to return a value")
	}
}
