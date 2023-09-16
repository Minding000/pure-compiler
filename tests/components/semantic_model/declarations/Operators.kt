package components.semantic_model.declarations

import logger.Severity
import logger.issues.declaration.AbstractMemberInNonAbstractTypeDefinition
import logger.issues.declaration.MissingImplementations
import logger.issues.declaration.Redeclaration
import logger.issues.declaration.VariadicParameterInOperator
import logger.issues.modifiers.OverriddenSuperMissing
import logger.issues.modifiers.OverridingFunctionReturnTypeNotAssignable
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull

internal class Operators {

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
	fun `allows abstract classes to contain abstract operators`() {
		val sourceCode =
			"""
				Int class
				abstract IntList class {
					abstract operator[index: Int]: Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMemberInNonAbstractTypeDefinition>()
	}

	@Test
	fun `disallows non-abstract classes to contain abstract operators`() {
		val sourceCode =
			"""
				Int class
				IntList class {
					abstract operator[index: Int]: Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<AbstractMemberInNonAbstractTypeDefinition>(
			"Abstract member '[Int]: Int' is not allowed in non-abstract type declaration 'IntList'.", Severity.ERROR)
	}

	@Test
	fun `allows abstract classes to not override abstract operators`() {
		val sourceCode =
			"""
				Int class
				abstract IntList class {
					abstract operator[index: Int]: Int
				}
				abstract VoidIntList class: IntList
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows non-abstract classes to not override non-abstract operators`() {
		val sourceCode =
			"""
				Int class
				abstract IntList class {
					operator[index: Int]: Int
				}
				VoidIntList class: IntList
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows abstract classes to override abstract operators`() {
		val sourceCode =
			"""
				Int class
				abstract IntList class {
					abstract operator[index: Int]: Int
				}
				VoidIntList class: IntList {
					overriding operator[index: Int]: Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows abstract classes to override abstract operators of generic super type`() {
		val sourceCode =
			"""
				Int class
				abstract List class {
					containing Element
					abstract operator[index: Int]: Element
				}
				IntList class: <Int>List {
					overriding operator[index: Int]: Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `doesn't require generic type definitions to override abstract operators`() {
		val sourceCode =
			"""
				Int class
				abstract IntList class {
					abstract operator[index: Int]: Int
				}
				abstract VoidCollections class {
					containing VoidIntList: IntList
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `disallows non-abstract subclasses that don't implement inherited abstract operators`() {
		val sourceCode = """
			Int class
			String class
			abstract Collection class {
				abstract val size: Int
			}
			abstract List class: Collection {
				abstract operator[index: Int]: Int
				abstract operator[id: String]: Int
			}
			LinkedList class: List {
				overriding operator[index: Int]: Int
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingImplementations>(
			"""
				Non-abstract type declaration 'LinkedList' does not implement the following inherited members:
				 - Collection
				   - size: Int
				 - List
				   - [String]: Int
			""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `disallows variadic parameters in operators`() {
		val sourceCode =
			"""
				Window class
				House class {
					operator +=(...windows: ...Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<VariadicParameterInOperator>("Variadic parameter in operator definition.",
			Severity.ERROR)
	}

	@Test
	fun `allows overriding of operator with generic return type`() {
		val sourceCode =
			"""
				Int class
				Iterator class
				abstract Iterable class {
					containing IteratorImplementation: Iterator
					operator [index: Int]: IteratorImplementation
				}
				Range class: <Iterator>Iterable {
					overriding operator [index: Int]: Iterator {
						return Iterator()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OverridingFunctionReturnTypeNotAssignable>()
	}

	@Test
	fun `detects link from operator to super operator with different return type`() {
		val sourceCode =
			"""
				Int class
				Float class
				House class {
					operator[a: Int]: Int
				}
				WoodenHouse class: House {
					overriding operator[a: Int]: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val operator = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(operator)
		lintResult.assertIssueNotDetected<OverriddenSuperMissing>()
		lintResult.assertIssueDetected<OverridingFunctionReturnTypeNotAssignable>(
			"Return type of overriding operator 'WoodenHouse[Int]: Float' is not assignable to " +
				"the return type of the overridden operator 'House[Int]: Int'.", Severity.ERROR)
	}
}
