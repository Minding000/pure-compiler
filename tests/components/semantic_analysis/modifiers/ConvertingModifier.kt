package components.semantic_analysis.modifiers

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class ConvertingModifier {

	@Test
	fun `is allowed on initializers`() {
		val sourceCode =
			"""
				Float class {
					converting init(int: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Modifier 'converting' is not allowed here")
	}

	@Test
	fun `allows initializers with one parameter`() {
		val sourceCode =
			"""
				Float class {
					converting init(int: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Converting initializers have to take exactly one parameter")
	}

	@Test
	fun `disallows initializers with no parameters`() {
		val sourceCode =
			"""
				Float class {
					converting init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Converting initializers have to take exactly one parameter")
	}

	@Test
	fun `disallows initializers with more than one parameter`() {
		val sourceCode =
			"""
				Float class {
					converting init(int: Int, int: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Converting initializers have to take exactly one parameter")
	}

	@Test
	fun `allows initializers without type parameters`() {
		val sourceCode =
			"""
				Float class {
					converting init(int: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Converting initializers cannot take type parameters")
	}

	@Test
	fun `disallows initializers with type parameters`() {
		val sourceCode =
			"""
				Float class {
					converting init(I; int: I)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Converting initializers cannot take type parameters")
	}

	@Test
	fun `allows for conversion between types in declarations`() {
		val sourceCode =
			"""
				Int class
				Float class {
					converting init(int: Int)
				}
				val original = Int()
				val converted: Float = original
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "not assignable")
	}

	@Test
	fun `allows for conversion between types in assignments`() {
		val sourceCode =
			"""
				Int class
				Float class {
					converting init(int: Int)
				}
				val original = Int()
				var converted: Float
				converted = original
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "not assignable")
	}

	@Test
	fun `allows for unambiguous conversion between types`() {
		val sourceCode =
			"""
				Int class
				Float class {
					converting init(int: Int)
				}
				Int64 class
				val original = Int()
				val converted: Float | Int64 = original
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"needs to be explicit, because there are multiple possible conversions")
	}

	@Test
	fun `disallows for ambiguous conversion between types`() {
		val sourceCode =
			"""
				Int class
				Float class {
					converting init(int: Int)
				}
				Int64 class {
					converting init(int: Int)
				}
				val original = Int()
				val converted: Float | Int64 = original
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, """
			Conversion from 'Int' to 'Float | Int64' needs to be explicit, because there are multiple possible conversions:
			 - Float
			 - Int64
		""".trimIndent())
	}
}
