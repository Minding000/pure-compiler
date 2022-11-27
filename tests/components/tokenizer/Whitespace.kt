package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class Whitespace {

	@Test
	fun `detects line break tokens`() {
		TestUtil.assertTokenType("\n", WordAtom.LINE_BREAK)
	}

	@Test
	fun `ignores other whitespace`() {
		TestUtil.assertTokenIsIgnored(" ")
		TestUtil.assertTokenIsIgnored("\t")
		TestUtil.assertTokenIsIgnored("\r")
	}
}
