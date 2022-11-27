package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

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
	fun `detects string tokens`() {
		TestUtil.assertTokenType(""" "" """.trim(), WordAtom.STRING_LITERAL)
		TestUtil.assertTokenType(""" "Hello world!" """.trim(), WordAtom.STRING_LITERAL)
		TestUtil.assertTokenType(""" "Hi my name is 'Nara'." """.trim(), WordAtom.STRING_LITERAL)
		TestUtil.assertTokenType("\"Hi my name is \\\"Nara\\\".\"", WordAtom.STRING_LITERAL)
	}
}
