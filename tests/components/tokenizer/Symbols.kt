package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class Symbols {

	@Test
	fun `detects assignment tokens`() {
		TestUtil.assertTokenType("=", WordAtom.ASSIGNMENT)
	}

	@Test
	fun `detects comma tokens`() {
		TestUtil.assertTokenType(",", WordAtom.COMMA)
	}

	@Test
	fun `detects semicolon tokens`() {
		TestUtil.assertTokenType(";", WordAtom.SEMICOLON)
	}

	@Test
	fun `detects colon tokens`() {
		TestUtil.assertTokenType(":", WordAtom.COLON)
	}

	@Test
	fun `detects question mark tokens`() {
		TestUtil.assertTokenType("?", WordAtom.QUESTION_MARK)
	}

	@Test
	fun `detects opening parentheses tokens`() {
		TestUtil.assertTokenType("(", WordAtom.OPENING_PARENTHESIS)
	}

	@Test
	fun `detects closing parentheses tokens`() {
		TestUtil.assertTokenType(")", WordAtom.CLOSING_PARENTHESIS)
	}

	@Test
	fun `detects opening brackets tokens`() {
		TestUtil.assertTokenType("[", WordAtom.OPENING_BRACKET)
	}

	@Test
	fun `detects closing brackets tokens`() {
		TestUtil.assertTokenType("]", WordAtom.CLOSING_BRACKET)
	}

	@Test
	fun `detects opening braces tokens`() {
		TestUtil.assertTokenType("{", WordAtom.OPENING_BRACE)
	}

	@Test
	fun `detects closing braces tokens`() {
		TestUtil.assertTokenType("}", WordAtom.CLOSING_BRACE)
	}
}
