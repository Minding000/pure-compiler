package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class ErrorHandling {

	@Test
	fun `detects optional try tokens`() {
		TestUtil.assertTokenType("try?", WordAtom.OPTIONAL_TRY)
	}

	@Test
	fun `detects unchecked try tokens`() {
		TestUtil.assertTokenType("try!", WordAtom.UNCHECKED_TRY)
	}

	@Test
	fun `detects handle tokens`() {
		TestUtil.assertTokenType("handle", WordAtom.HANDLE)
	}

	@Test
	fun `detects always tokens`() {
		TestUtil.assertTokenType("always", WordAtom.ALWAYS)
	}
}
