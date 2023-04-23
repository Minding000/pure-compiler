package components.semantic_analysis.declarations

import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.VariableValue
import logger.Severity
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.definition.*
import logger.issues.modifiers.DisallowedModifier
import logger.issues.modifiers.DuplicateModifier
import logger.issues.modifiers.NoParentToBindTo
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class Declarations {

	@Test
	fun `emits error for incompatible source expression type`() {
		val sourceCode =
			"""
				Toast class
				Banana object
				var toast: Toast = Banana
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<TypeNotAssignable>("Type 'Banana' is not assignable to type 'Toast'.", Severity.ERROR)
	}

	@Test
	fun `emits error if no type is provided to variable declaration`() {
		val sourceCode =
			"""
				var toast
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DeclarationMissingTypeOrValue>(
			"Declaration requires a type or value to infer a type from.", Severity.ERROR)
	}

	@Test
	fun `detects shadowed variables`() {
		val sourceCode =
			"""
				Handler class
				val defaultHandler: Handler
				Event class {
					const defaultHandler: Handler
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ShadowsElement>(
			"'defaultHandler' shadows a member, previously declared in Test.Test:2:4.", Severity.WARNING)
	}

	@Test
	fun `allows variable declarations`() {
		val sourceCode =
			"""
				Car class
				var car1: Car
				var car2: Car
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<Redeclaration>()
	}

	@Test
	fun `detects variable redeclarations`() {
		val sourceCode =
			"""
				Car class
				var car: Car
				val car: Car
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<Redeclaration>("Redeclaration of value 'car', previously declared in Test.Test:2:4.",
			Severity.ERROR)
	}

	@Test
	fun `allows type declarations`() {
		val sourceCode =
			"""
				Animal class
				AnimalType enum
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<Redeclaration>()
	}

	@Test
	fun `detects type redeclarations`() {
		val sourceCode =
			"""
				Animal class
				Animal enum
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<Redeclaration>("Redeclaration of value 'Animal', previously declared in Test.Test:1:7.",
			Severity.ERROR)
	}

	@Test
	fun `allows function declarations`() {
		val sourceCode =
			"""
				Int class
				Human class {
					to push()
					to push(pressure: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<Redeclaration>()
	}

	@Test
	fun `detects function redeclarations`() {
		val sourceCode =
			"""
				Pressure class
				alias P = Pressure
				Human class {
					to push(): Pressure
					to push(pressure: P)
					to push(pressure: Pressure)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<Redeclaration>(
			"Redeclaration of function 'Human.push(Pressure)', previously declared in Test.Test:5:4.", Severity.ERROR)
	}

	@Test
	fun `allows operator declarations`() {
		val sourceCode =
			"""
				Time class
				Mood class
				Human class {
					operator [time: Time]: Mood
					operator [start: Time, end: Time]: Mood
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<Redeclaration>()
	}

	@Test
	fun `detects operator redeclarations`() {
		val sourceCode =
			"""
				Time class
				alias T = Time
				Human class {
					operator [start: T, end: T](time: T)
					operator [time: T]: T
					operator [time: Time]: Time
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<Redeclaration>(
			"Redeclaration of operator 'Human[Time]: Time', previously declared in Test.Test:5:10.", Severity.ERROR)
	}

	@Test
	fun `detects invalid modifiers`() {
		val sourceCode =
			"""
				overriding House class
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>("Modifier 'overriding' is not allowed here.", Severity.WARNING)
	}

	@Test
	fun `detects duplicate modifiers`() {
		val sourceCode =
			"""
				native native Memory class
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DuplicateModifier>("Duplicate 'native' modifier.", Severity.WARNING)
	}

	@Test
	fun `allows handle blocks to declare error variable`() {
		val sourceCode =
			"""
				IOError class
				Config class {
					to saveToDisk() {
					} handle error: IOError {
						error
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variable = lintResult.find<VariableValue> { variableValue -> variableValue.name == "error" }
		assertNotNull(variable?.type)
	}

	@Test
	fun `emits warning for generic non-index operator`() {
		val sourceCode = """
			Vector class {
				operator +(ReturnType; other: Self): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<GenericOperator>("Operators (except for the index operator) can not be generic.",
			Severity.WARNING)
	}

	@Test
	fun `emits warning for generic parameters in parentheses in index operator`() {
		val sourceCode = """
			Vector class {
				operator [key: IndexType](IndexType; value: Int)
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<TypeParametersOutsideOfIndexParameterList>(
			"Type parameters for the index operator are received in the index parameter list instead.", Severity.WARNING)
	}

	@Test
	fun `allows unbound classes`() {
		val sourceCode =
			"""
				Tree class
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NoParentToBindTo>()
	}

	@Test
	fun `allows classes to be bound if they have a parent`() {
		val sourceCode =
			"""
				Parent class {
					bound Child class
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NoParentToBindTo>()
	}

	@Test
	fun `disallows classes to be bound if they don't have a parent`() {
		val sourceCode =
			"""
				bound Child class
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<NoParentToBindTo>("Can't bind type definition, because it doesn't have a parent.",
			Severity.WARNING)
	}

	@Test
	fun `allows type definitions to inherit from other type definitions`() {
		val sourceCode =
			"""
				Tool class
				Drill class: Tool
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<CircularInheritance>()
	}

	@Test
	fun `disallows type definitions to inherit from themself directly`() {
		val sourceCode =
			"""
				Pen class: Pen {
					Type class
					val value: Type
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<CircularInheritance>("Type definitions cannot inherit from themself.", Severity.ERROR)
	}

	@Test
	fun `disallows type definitions to inherit from themself indirectly`() {
		val sourceCode =
			"""
				Egg class: Chicken
				Chicken class: Egg
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<CircularInheritance>()
	}

	@Test
	fun `allows explicit parent types on unscoped type definitions`() {
		val sourceCode =
			"""
				Editor class
				Theme class in Editor
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ExplicitParentOnScopedTypeDefinition>()
		val typeDefinition = lintResult.find<TypeDefinition> { typeDefinition -> typeDefinition.name == "Theme" }
		assertEquals("Editor", typeDefinition?.parentTypeDefinition?.name)
	}

	@Test
	fun `disallows explicit parent types on scoped type definitions`() {
		val sourceCode =
			"""
				Editor class {
					Theme class in Editor
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ExplicitParentOnScopedTypeDefinition>(
			"Explicit parent types are only allowed on unscoped type definitions.", Severity.ERROR)
	}
}
