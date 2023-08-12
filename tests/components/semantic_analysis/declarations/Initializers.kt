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
import kotlin.test.assertNull

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
	fun `allows initializer declarations`() {
		val sourceCode =
			"""
				Tank class {
					containing Liquid
					init
					init(liquid: Liquid)
				}
				Water class
				Hydrogen class
				<Water>Tank()
				<Hydrogen>Tank()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<Redeclaration>()
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
			"Abstract member 'init(Int)' is not allowed in non-abstract type declaration 'Plant'.", Severity.ERROR)
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
	fun `doesn't require generic type definitions to override abstract initializers`() {
		val sourceCode =
			"""
				Int class
				abstract Plant class {
					abstract init(size: Int)
				}
				abstract Garden class {
					containing Tree: Plant
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
			Non-abstract type declaration 'Tree' does not implement the following inherited members:
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

	@Test
	fun `allows single variadic parameter`() {
		val sourceCode =
			"""
				Window class
				House class {
					init(...windows: ...Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MultipleVariadicParameters>()
		lintResult.assertIssueNotDetected<InvalidVariadicParameterPosition>()
	}

	@Test
	fun `detects multiple variadic parameters`() {
		val sourceCode =
			"""
				Window class
				House class {
					init(...openWindows: ...Window, ...closedWindows: ...Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MultipleVariadicParameters>("Signatures can have at most one variadic parameter.",
			Severity.ERROR)
	}

	@Test
	fun `detects variadic parameters not positioned at the parameter list end`() {
		val sourceCode =
			"""
				Window class
				House class {
					init(...windows: ...Window, selectedWindow: Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<InvalidVariadicParameterPosition>("Variadic parameters have to be the last parameter.",
			Severity.ERROR)
	}

	@Test
	fun `links initializer without parameters to super initializer`() {
		val sourceCode =
			"""
				House class {
					init()
				}
				WoodenHouse class: House {
					overriding init()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializer = lintResult.find<InitializerDefinition>(InitializerDefinition::isOverriding)
		assertNotNull(initializer)
		assertNotNull(initializer.superInitializer)
	}

	@Test
	fun `links initializer to super initializer with identically typed parameter`() {
		val sourceCode =
			"""
				Int class
				House class {
					init(a: Int)
				}
				WoodenHouse class: House {
					overriding init(b: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializer = lintResult.find<InitializerDefinition>(InitializerDefinition::isOverriding)
		assertNotNull(initializer)
		assertNotNull(initializer.superInitializer)
	}

	@Test
	fun `links initializer to super initializer with identically typed parameter in second order parent`() {
		val sourceCode =
			"""
				Int class
				House class {
					init
					init(a: Int)
				}
				NaturalHouse class: House
				WoodenHouse class: NaturalHouse {
					overriding init(a: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializer = lintResult.find<InitializerDefinition>(InitializerDefinition::isOverriding)
		assertNotNull(initializer)
		assertNotNull(initializer.superInitializer)
	}

	@Test
	fun `doesn't link initializer to super initializer with differently typed parameter`() {
		val sourceCode =
			"""
				Int class
				Float class
				House class {
					init(a: Int)
				}
				WoodenHouse class: House {
					overriding init(a: Float)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializer = lintResult.find<InitializerDefinition>(InitializerDefinition::isOverriding)
		assertNotNull(initializer)
		assertNull(initializer.superInitializer)
	}

	@Test
	fun `links initializer to super initializer with super-type parameter`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				House class {
					init(a: Int)
				}
				WoodenHouse class: House {
					overriding init(a: Number)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializer = lintResult.find<InitializerDefinition>(InitializerDefinition::isOverriding)
		assertNotNull(initializer)
		assertNotNull(initializer.superInitializer)
	}

	@Test
	fun `doesn't link initializer to super initializer with sub-type parameter`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				House class {
					init(a: Number)
				}
				WoodenHouse class: House {
					overriding init(a: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializer = lintResult.find<InitializerDefinition>(InitializerDefinition::isOverriding)
		assertNotNull(initializer)
		assertNull(initializer.superInitializer)
	}

	@Test
	fun `links initializer to super initializer with identically typed variadic parameter`() {
		val sourceCode =
			"""
				Int class
				House class {
					init(...a: ...Int)
				}
				WoodenHouse class: House {
					overriding init(...a: ...Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializer = lintResult.find<InitializerDefinition>(InitializerDefinition::isOverriding)
		assertNotNull(initializer)
		assertNotNull(initializer.superInitializer)
	}

	@Test
	fun `doesn't link initializer to super initializer with differently typed variadic parameter`() {
		val sourceCode =
			"""
				Int class
				Float class
				House class {
					init(...a: ...Int)
				}
				WoodenHouse class: House {
					overriding init(...a: ...Float)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializer = lintResult.find<InitializerDefinition>(InitializerDefinition::isOverriding)
		assertNotNull(initializer)
		assertNull(initializer.superInitializer)
	}

	@Test
	fun `doesn't link variadic initializer to non-variadic super initializer`() {
		val sourceCode =
			"""
				Int class
				House class {
					init()
				}
				WoodenHouse class: House {
					overriding init(...a: ...Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val initializer = lintResult.find<InitializerDefinition>(InitializerDefinition::isOverriding)
		assertNotNull(initializer)
		assertNull(initializer.superInitializer)
	}
}
