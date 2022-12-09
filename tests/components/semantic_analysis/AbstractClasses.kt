package components.semantic_analysis

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class AbstractClasses {

	@Test
	fun `emits error for abstract member in non-abstract class`() {
		val sourceCode = """
			Int class {}
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
	fun `doesn't emit error for abstract member in abstract class`() {
		val sourceCode = """
			Int class {}
			abstract List class {
				abstract val id: Int
				abstract to clear()
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not allowed in non-abstract class")
	}

	@Test
	fun `doesn't emit error for non-abstract member in non-abstract class`() {
		val sourceCode = """
			Int class {
				init
			}
			List class {
				val id = Int()
				to clear()
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not allowed in non-abstract class")
	}

	@Test
	fun `emits error for instantiation of abstract classes`() {
		val sourceCode = """
			abstract List class {
				init
			}
			List()
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Abstract class 'List' cannot be instantiated")
	}

	@Test
	fun `emits error for non-abstract subclasses that don't implement inherited abstract members`() {
		val sourceCode = """
			Int class {}
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
