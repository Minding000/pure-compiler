package components.semantic_model.declarations

import logger.Severity
import logger.issues.declaration.*
import logger.issues.modifiers.OverriddenSuperMissing
import logger.issues.modifiers.OverridingFunctionReturnTypeNotAssignable
import logger.issues.modifiers.OverridingMemberKindMismatch
import logger.issues.modifiers.OverridingPropertyTypeMismatch
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class Functions {

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
	fun `allows abstract classes to contain abstract functions`() {
		val sourceCode =
			"""
				abstract Plant class {
					abstract to water()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMemberInNonAbstractTypeDefinition>()
	}

	@Test
	fun `disallows non-abstract classes to contain abstract functions`() {
		val sourceCode =
			"""
				Plant class {
					abstract to water()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<AbstractMemberInNonAbstractTypeDefinition>(
			"Abstract member 'water()' is not allowed in non-abstract type declaration 'Plant'.", Severity.ERROR)
	}

	@Test
	fun `allows abstract classes to not override abstract functions`() {
		val sourceCode =
			"""
				abstract Plant class {
					abstract to water()
				}
				abstract Tree class: Plant
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows non-abstract classes to not override non-abstract functions`() {
		val sourceCode =
			"""
				abstract Plant class {
					to water()
				}
				Tree class: Plant
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows classes to override abstract functions`() {
		val sourceCode =
			"""
				abstract Plant class {
					abstract to water()
				}
				Tree class: Plant {
					overriding to water()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows classes to not override abstract functions overridden by a super type`() {
		val sourceCode =
			"""
				abstract WaterConsumer class {
					abstract to water()
				}
				abstract TallWaterConsumer class: WaterConsumer
				abstract Plant class {
					abstract to water()
				}
				Tree class: Plant {
					overriding to water()
				}
				TallTree class: Tree & TallWaterConsumer
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows classes to override abstract functions of generic super type`() {
		val sourceCode =
			"""
				Int class
				abstract List class {
					containing Element
					abstract to add(element: Element)
				}
				IntList class: <Int>List {
					overriding to add(element: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `doesn't require generic type definitions to override abstract functions`() {
		val sourceCode =
			"""
				abstract Plant class {
					abstract to water()
				}
				abstract Garden class {
					containing Tree: Plant
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `disallows non-abstract subclasses that don't implement inherited abstract functions`() {
		val sourceCode = """
			Int class
			abstract Collection class {
				abstract val size: Int
			}
			abstract List class: Collection {
				abstract to clear()
				abstract to clear(position: Int)
			}
			LinkedList class: List {
				overriding to clear()
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingImplementations>(
			"""
				Non-abstract type declaration 'LinkedList' does not implement the following inherited members:
				 - Collection
				   - size: Int
				 - List
				   - clear(Int)
			""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `allows single variadic parameter`() {
		val sourceCode =
			"""
				Window class
				House object {
					to add(...windows: ...Window)
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
				House object {
					to add(...openWindows: ...Window, ...closedWindows: ...Window)
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
				House object {
					to add(...windows: ...Window, selectedWindow: Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<InvalidVariadicParameterPosition>("Variadic parameters have to be the last parameter.",
			Severity.ERROR)
	}

	@Test
	fun `links function without parameters to identically named super function`() {
		val sourceCode =
			"""
				House class {
					to build()
				}
				WoodenHouse class: House {
					overriding to build()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNotNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `links function without parameters to identically named super function in second order parent`() {
		val sourceCode =
			"""
				House class {
					to build()
				}
				NaturalHouse class: House
				WoodenHouse class: NaturalHouse {
					overriding to build()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNotNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `doesn't link function without parameters to differently named super function`() {
		val sourceCode =
			"""
				House class {
					to build()
				}
				WoodenHouse class: House {
					overriding to demolish()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `links function to super function with identically typed parameter`() {
		val sourceCode =
			"""
				Int class
				House class {
					to build(a: Int)
				}
				WoodenHouse class: House {
					overriding to build(b: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNotNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `doesn't link function to super function with differently typed parameter`() {
		val sourceCode =
			"""
				Int class
				Float class
				House class {
					to build(a: Int)
				}
				WoodenHouse class: House {
					overriding to build(a: Float)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `allows overriding of function with generic return type`() {
		val sourceCode =
			"""
				Iterator class
				abstract Iterable class {
					containing IteratorImplementation: Iterator
					to createIterator(): IteratorImplementation
				}
				Range class: <Iterator>Iterable {
					overriding to createIterator(): Iterator {
						return Iterator()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OverridingFunctionReturnTypeNotAssignable>()
	}

	@Test
	fun `detects link from function to super function with different return type`() {
		val sourceCode =
			"""
				Int class
				Float class
				House class {
					to build(a: Int): Int
				}
				WoodenHouse class: House {
					overriding to build(a: Int): Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		lintResult.assertIssueNotDetected<OverriddenSuperMissing>()
		lintResult.assertIssueDetected<OverridingFunctionReturnTypeNotAssignable>(
			"Return type of overriding function 'WoodenHouse.build(Int): Float' is not assignable to " +
				"the return type of the overridden function 'House.build(Int): Int'.", Severity.ERROR)
	}

	@Test
	fun `links function to super function with super-type parameter`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				House class {
					to build(a: Int)
				}
				WoodenHouse class: House {
					overriding to build(a: Number)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNotNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `doesn't link function to super function with sub-type parameter`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				House class {
					to build(a: Number)
				}
				WoodenHouse class: House {
					overriding to build(a: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `links function to super function with identically typed variadic parameter`() {
		val sourceCode =
			"""
				Int class
				House class {
					to build(...a: ...Int)
				}
				WoodenHouse class: House {
					overriding to build(...a: ...Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNotNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `doesn't link function to super function with differently typed variadic parameter`() {
		val sourceCode =
			"""
				Int class
				Float class
				House class {
					to build(...a: ...Int)
				}
				WoodenHouse class: House {
					overriding to build(...a: ...Float)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `doesn't link variadic function to non-variadic super function`() {
		val sourceCode =
			"""
				Int class
				House class {
					to build()
				}
				WoodenHouse class: House {
					overriding to build(...a: ...Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val function = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(function)
		assertNull(function.signature.superFunctionSignature)
	}

	@Test
	fun `allows functions to overwrite functions`() {
		val sourceCode = """
			Computer class {
				to result()
			}
			ClassicalComputer class: Computer {
				overriding to result()
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OverridingPropertyTypeMismatch>()
	}

	@Test
	fun `disallows functions to overwrite properties`() {
		val sourceCode = """
			Computer class {
				val result = 1
			}
			ClassicalComputer class: Computer {
				overriding to result()
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingMemberKindMismatch>(
			"'result' function cannot override 'result' property.", Severity.ERROR)
	}

	@Test
	fun `disallows functions to overwrite computed properties`() {
		val sourceCode = """
			Computer class {
				computed result: Int gets 1
			}
			ClassicalComputer class: Computer {
				overriding to result()
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingMemberKindMismatch>(
			"'result' function cannot override 'result' computed property.", Severity.ERROR)
	}
}
