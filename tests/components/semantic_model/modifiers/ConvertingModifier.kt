package components.semantic_model.modifiers

import logger.Severity
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.modifiers.ConvertingInitializerTakingTypeParameters
import logger.issues.modifiers.ConvertingInitializerWithInvalidParameterCount
import logger.issues.modifiers.DisallowedModifier
import logger.issues.resolution.ConversionAmbiguity
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
		lintResult.assertIssueNotDetected<DisallowedModifier>()
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
		lintResult.assertIssueNotDetected<ConvertingInitializerWithInvalidParameterCount>()
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
		lintResult.assertIssueDetected<ConvertingInitializerWithInvalidParameterCount>(
			"Converting initializers have to take exactly one parameter.", Severity.ERROR)
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
		lintResult.assertIssueDetected<ConvertingInitializerWithInvalidParameterCount>()
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
		lintResult.assertIssueNotDetected<ConvertingInitializerTakingTypeParameters>()
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
		lintResult.assertIssueDetected<ConvertingInitializerTakingTypeParameters>(
			"Converting initializers cannot take type parameters.", Severity.ERROR)
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
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
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
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
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
		lintResult.assertIssueNotDetected<ConversionAmbiguity>()
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
		lintResult.assertIssueDetected<ConversionAmbiguity>("""
			Conversion from 'Int' to 'Float | Int64' needs to be explicit, because there are multiple possible conversions:
			 - 'Float(Int)' declared at Test.Test:3:12
			 - 'Int64(Int)' declared at Test.Test:6:12
		""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `allows for conversion to generic type`() {
		val sourceCode =
			"""
				Int class
				abstract Number class {
					abstract converting init(value: Int)
				}
				Range class {
					containing N: Number
					val start: N = Int()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
		lintResult.assertIssueNotDetected<ConversionAmbiguity>()
	}
}
