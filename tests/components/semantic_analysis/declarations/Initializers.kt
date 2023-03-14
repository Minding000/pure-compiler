package components.semantic_analysis.declarations

import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.Parameter
import logger.Severity
import logger.issues.definition.*
import logger.issues.modifiers.MissingOverridingKeyword
import logger.issues.modifiers.OverriddenSuperInitializerMissing
import logger.issues.modifiers.OverridingInitializerMissingConvertingKeyword
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class Initializers {

	@Test
	fun `allows initializers inside objects without parameters`() {
		val sourceCode =
			"""
				Root object {
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ObjectInitializerTakingTypeParameters>()
		lintResult.assertIssueNotDetected<ObjectInitializerTakingParameters>()
	}

	@Test
	fun `detects initializers inside objects with type parameters`() {
		val sourceCode =
			"""
				Root object {
					init(ChildType;)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ObjectInitializerTakingTypeParameters>(
			"Object initializers can not take type parameters.", Severity.ERROR)
	}

	@Test
	fun `detects initializers inside objects with parameters`() {
		val sourceCode =
			"""
				Root object {
					init(childCount: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ObjectInitializerTakingParameters>("Object initializers can not take parameters.",
			Severity.ERROR)
	}

	@Test
	fun `detects redeclarations of initializer signatures`() {
		val sourceCode =
			"""
				Trait class
				alias T = Trait
				Human class {
					init
					init(t: T)
					init(t: Trait)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<Redeclaration>(
			"Redeclaration of initializer 'Human(Trait)', previously declared in Test.Test:5:1.")
	}

	@Test
	fun `creates default initializer if no initializer is defined`() {
		val sourceCode =
			"""
				Human class
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val defaultInitializer = lintResult.find<InitializerDefinition>()
		assertNotNull(defaultInitializer)
	}

	@Test
	fun `resolves property parameters in initializers`() {
		val sourceCode =
			"""
				Int class
				Human class {
					val age: Int
					init(age)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<PropertyParameterOutsideOfInitializer>()
		lintResult.assertIssueNotDetected<PropertyParameterMismatch>()
		val parameter = lintResult.find<Parameter>()
		assertEquals("Int", parameter?.type.toString())
	}

	@Test
	fun `disallows property parameters without matching property`() {
		val sourceCode =
			"""
				Human class {
					init(age)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<PropertyParameterMismatch>("Property parameter doesn't match any property.",
			Severity.ERROR)
	}

	@Test
	fun `disallows property parameters outside of initializers`() {
		val sourceCode =
			"""
				Human class {
					val age: Int
					to set(age)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<PropertyParameterOutsideOfInitializer>("Property parameter is not allowed here.",
			Severity.ERROR)
	}

	@Test
	fun `allows abstract classes to contain abstract initializers`() {
		val sourceCode =
			"""
				Int class
				abstract Plant class {
					abstract init(size: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMemberInNonAbstractTypeDefinition>()
	}

	@Test
	fun `disallows non-abstract classes to contain abstract initializers`() {
		val sourceCode =
			"""
				Int class
				Plant class {
					abstract init(size: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<AbstractMemberInNonAbstractTypeDefinition>(
			"Abstract member 'init(Int)' is not allowed in non-abstract type definition 'Plant'.", Severity.ERROR)
	}

	@Test
	fun `allows abstract classes to not override abstract initializers`() {
		val sourceCode =
			"""
				Int class
				abstract Plant class {
					abstract init(size: Int)
				}
				abstract Tree class: Plant
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows non-abstract classes to not override non-abstract initializers`() {
		val sourceCode =
			"""
				Int class
				abstract Plant class {
					init(size: Int)
				}
				Tree class: Plant
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows abstract classes to override abstract initializers`() {
		val sourceCode =
			"""
				Int class
				abstract Plant class {
					abstract init(size: Int)
				}
				Tree class: Plant {
					overriding init(size: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `disallows non-abstract classes to not override abstract initializers`() {
		val sourceCode =
			"""
				Int class
				abstract Plant class {
					abstract init(size: Int)
				}
				Tree class: Plant
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingImplementations>("""
			Non-abstract class 'Tree' does not implement the following inherited members:
			 - Plant
			   - init(Int)
		""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `allows overriding converting initializers with converting initializer`() {
		val sourceCode =
			"""
				Int class
				abstract Number class {
					converting abstract init(value: Int)
				}
				Float class: Number {
					overriding converting init(value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OverridingInitializerMissingConvertingKeyword>()
	}

	@Test
	fun `allows overriding non-converting initializers with non-converting initializer`() {
		val sourceCode =
			"""
				Int class
				abstract Number class {
					abstract init(value: Int)
				}
				Float class: Number {
					overriding init(value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OverridingInitializerMissingConvertingKeyword>()
	}

	@Test
	fun `allows overriding non-converting initializers with converting initializer`() {
		val sourceCode =
			"""
				Float class
				abstract Number class {
					abstract init(value: Float)
				}
				Double class: Number {
					overriding converting init(value: Float)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OverridingInitializerMissingConvertingKeyword>()
	}

	@Test
	fun `disallows overriding converting initializers with non-converting initializer`() {
		val sourceCode =
			"""
				Int class
				abstract Number class {
					converting abstract init(value: Int)
				}
				Float class: Number {
					overriding init(value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingInitializerMissingConvertingKeyword>(
			"Overriding initializer of converting initializer needs to be converting.", Severity.ERROR)
	}

	@Test
	fun `allows for initializers to be overridden`() {
		val sourceCode =
			"""
				Int class
				abstract Number class {
					abstract init(value: Int)
				}
				Float class: Number {
					overriding init(value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingOverridingKeyword>()
		lintResult.assertIssueNotDetected<OverriddenSuperInitializerMissing>()
	}

	@Test
	fun `detects missing overriding keyword on initializers`() {
		val sourceCode =
			"""
				Int class
				abstract Number class {
					abstract init(value: Int)
				}
				Float class: Number {
					init(value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingOverridingKeyword>("Initializer 'Float(Int)' is missing the 'overriding' keyword.",
			Severity.WARNING)
	}

	@Test
	fun `detects overriding keyword being used without super initializer`() {
		val sourceCode =
			"""
				Float class: Number {
					overriding init(value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverriddenSuperInitializerMissing>(
			"'overriding' keyword is used, but the initializer doesn't have an abstract super initializer.", Severity.WARNING)
	}

	@Test
	fun `doesn't require overriding keyword on initializers with non-abstract initializer with same signature in super type`() {
		val sourceCode =
			"""
				Int class
				abstract Number class {
					init(value: Int)
				}
				Float class: Number {
					init(value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingOverridingKeyword>()
	}

	@Test
	fun `detects overriding keyword being used with non-abstract super initializer`() {
		val sourceCode =
			"""
				Int class
				abstract Number class {
					init(value: Int)
				}
				Float class: Number {
					overriding init(value: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverriddenSuperInitializerMissing>()
	}
}
