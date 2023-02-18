package components.tokenizer

import org.junit.jupiter.api.Test
import util.TestUtil

internal class TypeDeclarations {

	@Test
	fun `detects class tokens`() {
		TestUtil.assertTokenType("class", WordAtom.CLASS)
	}

	@Test
	fun `detects object tokens`() {
		TestUtil.assertTokenType("object", WordAtom.OBJECT)
	}

	@Test
	fun `detects enum tokens`() {
		TestUtil.assertTokenType("enum", WordAtom.ENUM)
	}

	@Test
	fun `detects type alias tokens`() {
		TestUtil.assertTokenType("alias", WordAtom.TYPE_ALIAS)
	}

	@Test
	fun `detects generic type tokens`() {
		TestUtil.assertTokenType("containing", WordAtom.CONTAINING)
	}

	@Test
	fun `detects parent type tokens`() {
		TestUtil.assertTokenType("in", WordAtom.IN)
	}
}
