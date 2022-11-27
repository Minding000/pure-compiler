package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class Statements {

	@Test
	fun `detects condition tokens`() {
		TestUtil.assertTokenType("if", WordAtom.IF)
		TestUtil.assertTokenType("else", WordAtom.ELSE)
	}

	@Test
	fun `detects switch tokens`() {
		TestUtil.assertTokenType("switch", WordAtom.SWITCH)
	}

	@Test
	fun `detects loop tokens`() {
		TestUtil.assertTokenType("loop", WordAtom.LOOP)
		TestUtil.assertTokenType("over", WordAtom.OVER)
		TestUtil.assertTokenType("while", WordAtom.WHILE)
		TestUtil.assertTokenType("break", WordAtom.BREAK)
		TestUtil.assertTokenType("next", WordAtom.NEXT)
	}

	@Test
	fun `detects return tokens`() {
		TestUtil.assertTokenType("return", WordAtom.RETURN)
	}

	@Test
	fun `detects yield tokens`() {
		TestUtil.assertTokenType("yield", WordAtom.YIELD)
	}

	@Test
	fun `detects raise tokens`() {
		TestUtil.assertTokenType("raise", WordAtom.RAISE)
	}
}
