package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class Casts {

	@Test
	fun `detects unchecked cast tokens`() {
		TestUtil.assertTokenType("as!", WordAtom.UNCHECKED_CAST)
	}

	@Test
	fun `detects optional cast tokens`() {
		TestUtil.assertTokenType("as?", WordAtom.OPTIONAL_CAST)
	}

	@Test
	fun `detects safe cast tokens`() {
		TestUtil.assertTokenType("as", WordAtom.AS)
	}

	@Test
	fun `detects conditional cast tokens`() {
		TestUtil.assertTokenType("is", WordAtom.IS)
	}

	@Test
	fun `detects negated conditional cast tokens`() {
		TestUtil.assertTokenType("is!", WordAtom.IS_NOT)
	}
}
