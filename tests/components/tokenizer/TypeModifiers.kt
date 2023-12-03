package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class TypeModifiers {

	@Test
	fun `detects consuming tokens`() {
		TestUtil.assertTokenType("consuming", WordAtom.CONSUMING)
	}

	@Test
	fun `detects producing tokens`() {
		TestUtil.assertTokenType("producing", WordAtom.PRODUCING)
	}

	@Test
	fun `detects spread tokens`() {
		TestUtil.assertTokenType("...", WordAtom.SPREAD)
	}

	@Test
	fun `detects and union tokens`() {
		TestUtil.assertTokenType("&", WordAtom.AND_UNION)
	}

	@Test
	fun `detects or union tokens`() {
		TestUtil.assertTokenType("|", WordAtom.OR_UNION)
	}
}
