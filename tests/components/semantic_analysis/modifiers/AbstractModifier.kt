package components.semantic_analysis.modifiers

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class AbstractModifier {

	@Test
	fun `is allowed on classes`() {
		val sourceCode = "abstract Goldfish class"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Modifier 'abstract' is not allowed here")
	}

	@Test
	fun `is not allowed on objects`() {
		val sourceCode = "abstract Earth object"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Modifier 'abstract' is not allowed here")
	}

	@Test
	fun `is not allowed on enums`() {
		val sourceCode = "abstract Tire enum"
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Modifier 'abstract' is not allowed here")
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
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Modifier 'abstract' is not allowed here")
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
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Modifier 'abstract' is not allowed here")
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
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Modifier 'abstract' is not allowed here")
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
		lintResult.assertMessageNotEmitted(Message.Type.WARNING,
			"Modifier 'abstract' is not allowed here")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not allowed in non-abstract class")
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
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Abstract member 'id: Int' is not allowed in non-abstract class 'List'")
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Abstract member 'clear()' is not allowed in non-abstract class 'List'")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not allowed in non-abstract class")
	}

	@Test
	fun `disallows instantiation of abstract classes`() {
		val sourceCode = """
			abstract List class
			List()
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Abstract class 'List' cannot be instantiated")
	}

	@Test
	fun `disallows non-abstract subclasses that don't implement inherited abstract members`() {
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
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"""
				Non-abstract class 'LinkedList' does not implement the following inherited members:
				 - Collection
				   - size: Int
				 - List
				   - clear(Int)
			""".trimIndent())
	}
}
