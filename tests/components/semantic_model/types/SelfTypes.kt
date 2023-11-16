package components.semantic_model.types

import logger.Severity
import logger.issues.declaration.InvalidSelfTypeLocation
import logger.issues.declaration.MissingSpecificOverrides
import logger.issues.modifiers.MissingSpecificKeyword
import logger.issues.resolution.CallToSpecificSuperMember
import org.junit.jupiter.api.Test
import util.TestUtil

internal class SelfTypes {

	@Test
	fun `self types can be used in classes`() {
		val sourceCode =
			"""
				Camera class {
					to getDevice(): Self
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<InvalidSelfTypeLocation>()
	}

	@Test
	fun `self types can not be used outside of classes`() {
		val sourceCode =
			"""
				val camera: Self
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<InvalidSelfTypeLocation>("The self type is only allowed in type declarations.",
			Severity.ERROR)
	}

	@Test
	fun `doesn't require function assigning this value to self type to be marked as specific`() {
		val sourceCode =
			"""
				abstract Backupable class {
					var backup: Self?
					abstract to storeBackup()
				}
				Shape class: Backupable {
					overriding to storeBackup() {
						backup = this
					}
				}
				Circle class: Shape
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingSpecificKeyword>()
	}

	@Test
	fun `detects function assigning its own type to self type without being marked as specific`() {
		val sourceCode =
			"""
				abstract Backupable class {
					var backup: Self?
					abstract to storeBackup()
				}
				Shape class: Backupable {
					overriding to storeBackup() {
						backup = Shape()
					}
				}
				Circle class: Shape
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingSpecificKeyword>(
			"Function 'Shape.storeBackup()' is missing the 'specific' keyword.", Severity.ERROR)
	}

	@Test
	fun `detects function use of its own type in self type context without being marked as specific`() {
		val sourceCode =
			"""
				abstract Backupable class {
					var backup: Self?
					abstract to storeBackup()
				}
				Shape class: Backupable {
					overriding to storeBackup() {
						store(Shape())
					}
					to store(self: Self) {
						backup = self
					}
				}
				Circle class: Shape
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingSpecificKeyword>(
			"Function 'Shape.storeBackup()' is missing the 'specific' keyword.", Severity.ERROR)
	}

	@Test
	fun `allows specific function to assign its own type to self type`() {
		val sourceCode =
			"""
				abstract Backupable class {
					var backup: Self?
					abstract to storeBackup()
				}
				Shape class: Backupable {
					specific overriding to storeBackup() {
						backup = Shape()
					}
				}
				Circle class: Shape {
					specific overriding to storeBackup() {
						backup = Circle()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingSpecificKeyword>()
	}

	@Test
	fun `detects type declaration inheriting from type with specific function that doesn't override it`() {
		val sourceCode =
			"""
				abstract Backupable class {
					var backup: Self?
					abstract to storeBackup()
				}
				Shape class: Backupable {
					overriding specific to storeBackup() {
						backup = Shape()
					}
				}
				Circle class: Shape
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingSpecificOverrides>("""
			Type declaration 'Circle' does not override the following specific members:
			 - Shape
			   - storeBackup()
		""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `detects super calls in specific functions`() {
		val sourceCode =
			"""
				abstract Backupable class {
					var backup: Self?
					abstract to storeBackup()
				}
				Shape class: Backupable {
					overriding specific to storeBackup() {
						backup = Shape()
					}
				}
				Circle class: Shape {
					overriding specific to storeBackup() {
						super.storeBackup()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<CallToSpecificSuperMember>("Super calls to specific functions are not allowed.",
			Severity.ERROR)
	}
}
