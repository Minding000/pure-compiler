package components.tokenizer

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Literals {

	@Test
	fun `detects null tokens`() {
		TestUtil.assertTokenType("null", WordAtom.NULL_LITERAL)
	}

	@Test
	fun `detects boolean tokens`() {
		TestUtil.assertTokenType("yes", WordAtom.BOOLEAN_LITERAL)
		TestUtil.assertTokenType("no", WordAtom.BOOLEAN_LITERAL)
	}

	@Test
	fun `detects number tokens`() {
		TestUtil.assertTokenType("8", WordAtom.NUMBER_LITERAL)
		TestUtil.assertTokenType("321", WordAtom.NUMBER_LITERAL)
		TestUtil.assertTokenType("987654321", WordAtom.NUMBER_LITERAL)
		TestUtil.assertTokenType("123_456_789", WordAtom.NUMBER_LITERAL)
		TestUtil.assertTokenType("4.1", WordAtom.NUMBER_LITERAL)
		TestUtil.assertTokenType("6.011_5", WordAtom.NUMBER_LITERAL)
		TestUtil.assertTokenType("7.900", WordAtom.NUMBER_LITERAL)
		TestUtil.assertTokenType("0", WordAtom.NUMBER_LITERAL)
		TestUtil.assertTokenType("5.2e3", WordAtom.NUMBER_LITERAL)
		TestUtil.assertTokenType("2.4e-10", WordAtom.NUMBER_LITERAL)
	}

	@Test
	fun `detects string start tokens`() {
		val stack = StateStack()
		TestUtil.assertTokenType(""" " """.trim(), WordAtom.STRING_START, stack)
		assertTrue(stack.isInString)
	}

	@Test
	fun `detects string end tokens and pops stack`() {
		val stack = StateStack()
		stack.push()
		TestUtil.assertTokenType(""" " """.trim(), WordAtom.STRING_END, stack)
		assertFalse(stack.isInString)
	}

	@Test
	fun `detects string segment tokens`() {
		val stack = StateStack()
		stack.push()
		TestUtil.assertTokenType(""" Hey there! """.trim(), WordAtom.STRING_SEGMENT, stack)
		TestUtil.assertTokenType(""" \" """.trim(), WordAtom.STRING_SEGMENT, stack)
		TestUtil.assertTokenType(""" \\\$ """.trim(), WordAtom.STRING_SEGMENT, stack)
		TestUtil.assertTokenType(""" \\\\\{ """.trim(), WordAtom.STRING_SEGMENT, stack)
		assertTrue(stack.isInString)
	}

	@Test
	fun `detects template expression start tokens`() {
		val stack = StateStack()
		stack.push()
		TestUtil.assertTokenType(""" { """.trim(), WordAtom.TEMPLATE_EXPRESSION_START, stack)
		assertFalse(stack.isInString)
		stack.push()
		TestUtil.assertTokenType(""" \\{ """.trim(), WordAtom.TEMPLATE_EXPRESSION_START, stack)
		assertFalse(stack.isInString)
	}

	@Test
	fun `pops state stack on template expression end`() {
		val stack = StateStack()
		stack.push()
		stack.push()
		TestUtil.assertTokenType(""" } """.trim(), WordAtom.CLOSING_BRACE, stack)
		assertTrue(stack.isInString)
	}

	@Test
	fun `counts open braces`() {
		val stack = StateStack()
		stack.push()
		stack.push()
		val project = TestUtil.createTestProject(""" { } } """.trim())
		val wordGenerator = WordGenerator(project, stack)
		wordGenerator.getNextWord()
		assertEquals(1, stack.openBraceCount)
		assertFalse(stack.isInString)
		wordGenerator.getNextWord()
		assertEquals(0, stack.openBraceCount)
		assertFalse(stack.isInString)
		wordGenerator.getNextWord()
		assertEquals(0, stack.openBraceCount)
		assertTrue(stack.isInString)
	}

	@Test
	fun `doesn't pop top-level state`() {
		val stack = StateStack()
		TestUtil.assertTokenType(""" } """.trim(), WordAtom.CLOSING_BRACE, stack)
		assertEquals(1, stack.list.size)
	}

	@Test
	fun `doesn't pop string state on closing brace`() {
		val stack = StateStack()
		stack.push()
		TestUtil.assertTokenType(""" } """.trim(), WordAtom.STRING_SEGMENT, stack)
		assertTrue(stack.isInString)
	}
}
