package components.semantic_model.modifiers

import logger.Severity
import logger.issues.declaration.AbstractMemberInNonAbstractTypeDefinition
import logger.issues.modifiers.AbstractClassInstantiation
import logger.issues.modifiers.DisallowedModifier
import org.junit.jupiter.api.Test
import util.TestUtil

internal class AbstractModifier {

	@Test
	fun `is allowed on classes`() {
		val sourceCode = "abstract Goldfish class"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on objects`() {
		val sourceCode = "abstract Earth object"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on enums`() {
		val sourceCode = "abstract Tire enum"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on initializers`() {
		val sourceCode =
			"""
				abstract Mask class {
					abstract init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on properties`() {
		val sourceCode =
			"""
				abstract Goldfish class {
					abstract val brain: Brain
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `is not allowed on computed properties`() {
		val sourceCode =
			"""
				abstract Goldfish class {
					abstract val name: String
						gets "Bernd"
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DisallowedModifier>()
	}

	@Test
	fun `is allowed on functions`() {
		val sourceCode =
			"""
				abstract Goldfish class {
					abstract to swim()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<DisallowedModifier>()
	}

	@Test
	fun `allows abstract members in abstract classes`() {
		val sourceCode = """
			Int class
			abstract List class {
				abstract val id: Int
				abstract to clear()
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMemberInNonAbstractTypeDefinition>()
	}

	@Test
	fun `disallows abstract members in non-abstract classes`() {
		val sourceCode = """
			Int class
			List class {
				abstract val id: Int
				abstract to clear()
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<AbstractMemberInNonAbstractTypeDefinition>(
			"Abstract member 'id: Int' is not allowed in non-abstract type declaration 'List'.", Severity.ERROR)
		lintResult.assertIssueDetected<AbstractMemberInNonAbstractTypeDefinition>(
			"Abstract member 'clear()' is not allowed in non-abstract type declaration 'List'.", Severity.ERROR)
	}

	@Test
	fun `disallows abstract members in objects`() {
		val sourceCode = """
			Int class
			System object {
				abstract val id: Int
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<AbstractMemberInNonAbstractTypeDefinition>(
			"Abstract member 'id: Int' is not allowed in non-abstract type declaration 'System'.", Severity.ERROR)
	}

	@Test
	fun `disallows abstract members in enums`() {
		val sourceCode = """
			Int class
			Status enum {
				abstract val id: Int
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<AbstractMemberInNonAbstractTypeDefinition>(
			"Abstract member 'id: Int' is not allowed in non-abstract type declaration 'Status'.", Severity.ERROR)
	}

	@Test
	fun `allows non-abstract members in non-abstract classes`() {
		val sourceCode = """
			Int class
			List class {
				val id = Int()
				to clear()
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMemberInNonAbstractTypeDefinition>()
	}

	@Test
	fun `allows instantiation of non-abstract classes`() {
		val sourceCode = """
			List class
			List()
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractClassInstantiation>()
	}

	@Test
	fun `disallows instantiation of abstract classes`() {
		val sourceCode = """
			abstract List class
			List()
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<AbstractClassInstantiation>("Abstract class 'List' cannot be instantiated.",
			Severity.ERROR)
	}
}
