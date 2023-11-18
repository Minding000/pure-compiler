package components.semantic_model.modifiers

import logger.Severity
import logger.issues.declaration.MissingSpecificOverrides
import logger.issues.modifiers.DisallowedModifier
import logger.issues.modifiers.MissingSpecificKeyword
import logger.issues.resolution.CallToSpecificSuperMember
import org.junit.jupiter.api.Test
import util.TestUtil

internal class SpecificModifier {

	@Test
	fun `is not allowed on classes`() {
		val sourceCode = "specific Goldfish class"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on objects`() {
		val sourceCode = "specific Earth object"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on enums`() {
		val sourceCode = "specific Tire enum"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on properties`() {
		val sourceCode =
			"""
				Goldfish class {
					specific val brain: Brain
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on computed properties`() {
		val sourceCode =
			"""
				Goldfish class {
					specific computed name: String
						gets "Bernd"
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on initializers`() {
		val sourceCode =
			"""
				Dictionary class {
					specific init()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on functions`() {
		val sourceCode =
			"""
				Goldfish class {
					specific to swim()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on operators`() {
		val sourceCode =
			"""
				Goldfish class {
					specific operator ++
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
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
