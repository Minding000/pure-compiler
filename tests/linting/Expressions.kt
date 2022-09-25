package linting

import linting.semantic_model.operations.Cast
import linting.semantic_model.operations.NullCheck
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Expressions {

	@Test
	fun `returns boolean from null checks`() {
		val sourceCode =
			"""
				val a: Int? = 5
				val b = a?
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val nullCheck = lintResult.find<NullCheck>()
		assertEquals(Linter.Literals.BOOLEAN, nullCheck?.type.toString())
	}

	@Test
	fun `detects unnecessary null checks`() {
		val sourceCode =
			"""
				val a: Int? = 5
				val b = a?
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Null check on required type is unnecessary.")
	}

	@Test
	fun `returns new type after force cast`() {
		val sourceCode =
			"""
				val a = 5
				a as! Float
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val cast = lintResult.find<Cast>()
		assertEquals(null, cast?.type)
	}

	@Test
	fun `returns optional new type after optional cast`() {
		val sourceCode =
			"""
				val a = 5
				a as? Float
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val cast = lintResult.find<Cast>()
		assertEquals(null, cast?.type)
	}
}