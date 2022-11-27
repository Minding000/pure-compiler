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
}
