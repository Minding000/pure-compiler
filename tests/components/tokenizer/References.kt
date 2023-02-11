package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class References {

	@Test
	fun `detects file reference tokens`() {
		TestUtil.assertTokenType("referencing", WordAtom.REFERENCING)
	}

	@Test
	fun `detects self reference tokens`() {
		TestUtil.assertTokenType("this", WordAtom.SELF_REFERENCE)
	}

	@Test
	fun `detects super reference tokens`() {
		TestUtil.assertTokenType("super", WordAtom.SUPER_REFERENCE)
	}

	@Test
	fun `detects identifier tokens`() {
		TestUtil.assertTokenType("SystemClock", WordAtom.IDENTIFIER)
		TestUtil.assertTokenType("a", WordAtom.IDENTIFIER)
		TestUtil.assertTokenType("a0z9", WordAtom.IDENTIFIER)
		TestUtil.assertTokenType("lower", WordAtom.IDENTIFIER)
		TestUtil.assertTokenType("UPPER", WordAtom.IDENTIFIER)
		TestUtil.assertTokenType("öäüéàßî", WordAtom.IDENTIFIER)
		TestUtil.assertTokenType("EXIT_SUCCESS", WordAtom.IDENTIFIER)
	}
}
